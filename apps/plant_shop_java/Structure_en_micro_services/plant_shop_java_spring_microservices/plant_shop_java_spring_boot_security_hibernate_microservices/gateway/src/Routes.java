package gateway.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import util.Request;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Handler principal de la Gateway.
 * Intercepte les requêtes et les redirige vers les microservices appropriés.
 */
final class GatewayHandler implements HttpHandler {

    private final GatewayConfig config;
    private final HttpClient http;
    GatewayHandler(GatewayConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    /**
     * Traite une requête HTTP entrante.
     * @param ex HttpExchange L'échange HTTP à traiter
     * @throws IOException En cas d'erreur d'entrée/sortie
     */
    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            forward(ex);
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, "{\"error\":\"Gateway failure\"}");
        }
    }

    /**
     * Redirige la requête vers le microservice cible.
     * @param ex HttpExchange L'échange HTTP à rediriger
     * @throws Exception En cas d'erreur de redirection
     */
    private void forward(HttpExchange ex) throws Exception {
        URI uri = ex.getRequestURI();
        String path = uri.getPath();
        if (!path.startsWith("/api")) {
            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
            return;
        }

        String targetPath = path.substring("/api".length());
        if (targetPath.isEmpty()) {
            targetPath = "/";
        }
        if (!targetPath.startsWith("/")) {
            targetPath = "/" + targetPath;
        }
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            targetPath = targetPath + "?" + uri.getRawQuery();
        }

        RouteTarget target = RouteTarget.resolve(targetPath);
        if (target == null) {
            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
            return;
        }

        SessionContext session = SessionContext.anonymous();
        boolean needAuth = config.requiresAuth(target.service(), ex.getRequestMethod(), target.path());
        if (!"auth".equals(target.service())) {
            session = resolveSession(ex);
            if (needAuth && !session.authenticated()) {
                sendJson(ex, 401, "{\"error\":\"Authentification requise\"}");
                return;
            }
        }

        byte[] body = ex.getRequestBody().readAllBytes();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(config.serviceUrl(target.service()) + target.path()))
            .method(ex.getRequestMethod(),
                body.length == 0
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(body));

        List<String> contentType = ex.getRequestHeaders().get("Content-Type");
        if (contentType != null && !contentType.isEmpty()) {
            builder.header("Content-Type", contentType.get(0));
        }

        Optional.ofNullable(ex.getRequestHeaders().getFirst("Cookie"))
            .ifPresent(cookie -> builder.header("Cookie", cookie));

        if (session.authenticated()) {
            builder.header("X-User-Id", String.valueOf(session.userId()));
            builder.header("X-User-Admin", String.valueOf(session.admin()));
        }

        HttpResponse<byte[]> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

        ex.getResponseHeaders().set("Content-Type",
            response.headers().firstValue("Content-Type").orElse("application/json"));

        response.headers().map().forEach((key, values) -> {
            if ("set-cookie".equalsIgnoreCase(key)) {
                for (String value : values) {
                    ex.getResponseHeaders().add("Set-Cookie", value);
                }
            }
        });

        ex.sendResponseHeaders(response.statusCode(), response.body().length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(response.body());
        }
    }

    /**
     * Résout le contexte de session depuis les cookies.
     * @param ex HttpExchange L'échange HTTP contenant les cookies
     * @return SessionContext Le contexte de session (authentifié ou anonyme)
     * @throws Exception En cas d'erreur de résolution
     */
    private SessionContext resolveSession(HttpExchange ex) throws Exception {
        String sessionId = Request.extractSessionId(ex);
        if (sessionId == null) {
            return SessionContext.anonymous();
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.serviceUrl("auth") + "/api/auth/_session"))
            .header("Cookie", "session_id=" + sessionId)
            .GET()
            .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return SessionContext.anonymous();
        }

        JSONObject json = new JSONObject(response.body());
        return new SessionContext(true, json.getInt("id"), json.optBoolean("admin", false));
    }

    /**
     * Envoie une réponse JSON.
     * @param ex HttpExchange L'échange HTTP
     * @param status int Le code de statut HTTP
     * @param body String Le corps de la réponse JSON
     * @throws IOException En cas d'erreur d'envoi
     */
    private static void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }
}

/**
 * Cible de routage vers un microservice.
 * @param service String Le nom du service cible
 * @param path String Le chemin de l'API
 */
record RouteTarget(String service, String path) {
    private static final String API_PREFIX = "/api";

    /**
     * Résout la cible de routage à partir du chemin.
     * @param path String Le chemin de l'API
     * @return RouteTarget La cible de routage ou null si non trouvée
     */
    static RouteTarget resolve(String path) {
        if (path.startsWith("/auth")) {
            return new RouteTarget("auth", API_PREFIX + path);
        }
        if (path.startsWith("/plants") || path.startsWith("/admin/plants")) {
            return new RouteTarget("catalog", API_PREFIX + path);
        }
        if (path.startsWith("/orders") || path.startsWith("/admin/orders")) {
            return new RouteTarget("order", API_PREFIX + path);
        }
        if (path.startsWith("/users") || path.startsWith("/admin/users")) {
            return new RouteTarget("user", API_PREFIX + path);
        }
        return null;
    }
}

/**
 * Contexte de session utilisateur.
 * @param authenticated boolean Vrai si l'utilisateur est authentifié
 * @param userId int L'ID de l'utilisateur (-1 si anonyme)
 * @param admin boolean Vrai si l'utilisateur est administrateur
 */
record SessionContext(boolean authenticated, int userId, boolean admin) {
    /**
     * Crée un contexte de session anonyme.
     * @return SessionContext Un contexte non authentifié
     */
    static SessionContext anonymous() {
        return new SessionContext(false, -1, false);
    }
}
