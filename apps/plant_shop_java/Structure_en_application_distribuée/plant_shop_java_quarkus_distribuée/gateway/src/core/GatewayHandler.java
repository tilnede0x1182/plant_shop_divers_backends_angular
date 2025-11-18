package core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import util.Request;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/**
 * Cœur de la Gateway :
 * 1. Reçoit la requête HTTP
 * 2. Gère le CORS (Preflight)
 * 3. Résout l'identité via auth-service (Session check)
 * 4. Trouve le bon microservice cible (RouteTarget)
 * 5. Transfère la requête (Proxy) en ajoutant les headers X-User-Id
 */
public class GatewayHandler implements HttpHandler {

    private final HttpClient httpClient;
    private final List<RouteTarget> routes;
    // Cache simple pour éviter d'appeler auth-service à chaque milliseconde pour le même cookie
    private final Map<String, SessionContext> sessionCache = new ConcurrentHashMap<>();
    // Configuration des ports des services backend
    private static final String AUTH_URL    = "http://localhost:6101";
    private static final String CATALOG_URL = "http://localhost:6102";
    private static final String ORDER_URL   = "http://localhost:6103";
    private static final String USER_URL    = "http://localhost:6104";

    public GatewayHandler() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        // Ordre important : les préfixes les plus spécifiques en premier si chevauchement
        this.routes = List.of(
            new RouteTarget("auth-service",    AUTH_URL,    "/api/auth"),
            new RouteTarget("catalog-admin",   CATALOG_URL, "/api/admin/plants"), // Admin d'abord
            new RouteTarget("catalog-service", CATALOG_URL, "/api/plants"), // Public
            new RouteTarget("order-service",   ORDER_URL,   "/api/orders"),
            new RouteTarget("user-admin",      USER_URL,    "/api/admin/users"), // Admin d'abord
            new RouteTarget("user-service",    USER_URL,    "/api/users")
        );
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // 1. Gestion CORS (Options)
        if (method.equalsIgnoreCase("OPTIONS")) {
            handleCors(exchange);
            return;
        }

        try {
            // 2. Routage
            RouteTarget target = routes.stream()
                    .filter(r -> r.matches(path))
                    .findFirst()
                    .orElse(null);

            if (target == null) {
                sendError(exchange, 404, "Gateway: Route not found for " + path);
                return;
            }

            // 3. Authentification (Résolution de session)
            SessionContext ctx = resolveSession(exchange);
            // 4. Proxy (Forward)
            proxyRequest(exchange, target, ctx);
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Gateway Error: " + e.getMessage());
        }
    }

    /**
     * Vérifie le cookie session_id auprès de l'auth-service.
     */
    private SessionContext resolveSession(HttpExchange exchange) {
        String sessionId = Request.extractSessionId(exchange);
        if (sessionId == null) {
            return SessionContext.anonymous();
        }

        // Vérification simple ou appel HTTP vers auth-service/me
        // Pour ce projet, on fait un appel à /api/auth/me pour valider le cookie
        try {
            HttpRequest authReq = HttpRequest.newBuilder()
                    .uri(URI.create(AUTH_URL + "/api/auth/me"))
                    .header("Cookie", "session_id=" + sessionId)
                    .GET()
                    .build();
            HttpResponse<String> authRes = httpClient.send(authReq, HttpResponse.BodyHandlers.ofString());

            if (authRes.statusCode() == 200) {
                // Parsing simple du JSON pour extraire id et admin
                // On suppose que le body est {"id": 1, "admin": true, ...}
                String body = authRes.body();
                int id = extractJsonInt(body, "id");
                boolean isAdmin = extractJsonBool(body, "admin");
                return new SessionContext(id, isAdmin);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Echec validation session: " + e.getMessage());
        }
        return SessionContext.anonymous();
    }

    private void proxyRequest(HttpExchange exchange, RouteTarget target, SessionContext ctx) throws IOException, InterruptedException {
        String targetUrl = target.resolveUrl(exchange.getRequestURI().toString());
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .method(exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofInputStream(() -> exchange.getRequestBody()));
        // Copie des headers pertinents
        exchange.getRequestHeaders().forEach((k, v) -> {
            if (!k.equalsIgnoreCase("Host") && !k.equalsIgnoreCase("Content-Length")) {
                v.forEach(val -> reqBuilder.header(k, val));
            }
        });
        // Injection de l'identité
        if (ctx.isAuthenticated()) {
            reqBuilder.header("X-User-Id", String.valueOf(ctx.userId()));
            reqBuilder.header("X-User-Admin", String.valueOf(ctx.isAdmin()));
        }

        HttpResponse<InputStream> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofInputStream());
        // Copie de la réponse vers le client
        exchange.getResponseHeaders().clear();
        response.headers().map().forEach((k, v) -> {
            // On filtre certains headers de transport
            if(!k.equalsIgnoreCase("Transfer-Encoding")) {
                v.forEach(val -> exchange.getResponseHeaders().add(k, val));
            }
        });
        // CORS Headers sur la réponse
        addCorsHeaders(exchange);

        exchange.sendResponseHeaders(response.statusCode(), 0);
        // 0 = chunked encoding si possible ou calcul auto
        try (InputStream is = response.body(); OutputStream os = exchange.getResponseBody()) {
            is.transferTo(os);
        }
    }

    private void handleCors(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:4200");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Cookie");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
    }

    private void sendError(HttpExchange ex, int code, String msg) throws IOException {
        byte[] bytes = msg.getBytes();
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Helpers JSON rudimentaires pour éviter de charger Jackson dans le handler si pas nécessaire
    private int extractJsonInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return 0;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Integer.parseInt(json.substring(start, end).trim());
    }

    private boolean extractJsonBool(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return false;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return Boolean.parseBoolean(json.substring(start, end).trim());
    }
}
