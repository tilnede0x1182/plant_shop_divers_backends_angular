package gateway.core;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Handler principal de la gateway.
 */
final class GatewayHandler implements HttpHandler {

    private final GatewayConfig config;
    private final HttpClient http;
    private final CorsSupport cors = new CorsSupport();

    /**
     * Construit le handler avec la config et le client HTTP.
     * @param config Configuration de la gateway
     * @param http Client HTTP
     */
    GatewayHandler(GatewayConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    /**
     * Gère une requête HTTP entrante.
     * @param ex Échange HTTP
     * @throws IOException En cas d'erreur
     */
    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (cors.handlePreflight(ex)) {
                return;
            }
            forward(ex);
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, "{\"error\":\"Gateway failure\"}");
        }
    }

    /**
     * Transmet une requête au service backend approprié.
     * @param ex Échange HTTP
     * @throws Exception En cas d'erreur
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

        cors.apply(ex);
        ex.sendResponseHeaders(response.statusCode(), response.body().length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(response.body());
        }
    }

    /**
     * Résout la session depuis les cookies.
     * @param ex Échange HTTP
     * @return Contexte de session
     * @throws Exception En cas d'erreur
     */
    private SessionContext resolveSession(HttpExchange ex) throws Exception {
        String sessionId = extractSessionId(ex);
        if (sessionId == null) {
            return SessionContext.anonymous();
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.serviceUrl("auth") + "/auth/_session"))
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
     * Extrait l'ID de session des cookies.
     * @param ex Échange HTTP
     * @return ID de session ou null
     */
    private static String extractSessionId(HttpExchange ex) {
        String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return null;
        }

        return Stream.of(cookieHeader.split(";"))
            .map(String::trim)
            .filter(cookie -> cookie.startsWith("session_id="))
            .map(cookie -> cookie.substring("session_id=".length()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Envoie une réponse JSON.
     * @param ex Échange HTTP
     * @param status Code de statut
     * @param body Corps JSON
     * @throws IOException En cas d'erreur
     */
    private void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        cors.apply(ex);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }
}

/**
 * Cible de routage vers un service.
 * @param service Nom du service
 * @param path Chemin de la route
 */
record RouteTarget(String service, String path) {
    /**
     * Résout le service cible depuis le chemin.
     * @param path Chemin de la requête
     * @return Cible de routage ou null
     */
    static RouteTarget resolve(String path) {
        if (path.startsWith("/auth")) {
            return new RouteTarget("auth", path);
        }
        if (path.startsWith("/plants") || path.startsWith("/admin/plants")) {
            return new RouteTarget("catalog", path);
        }
        if (path.startsWith("/orders") || path.startsWith("/admin/orders")) {
            return new RouteTarget("order", path);
        }
        if (path.startsWith("/users") || path.startsWith("/admin/users")) {
            return new RouteTarget("user", path);
        }
        return null;
    }
}

/**
 * Contexte de session utilisateur.
 * @param authenticated true si authentifié
 * @param userId ID de l'utilisateur
 * @param admin true si admin
 */
record SessionContext(boolean authenticated, int userId, boolean admin) {
    /**
     * Retourne un contexte anonyme.
     * @return Contexte anonyme
     */
    static SessionContext anonymous() {
        return new SessionContext(false, -1, false);
    }
}

/**
 * Support CORS pour la gateway.
 */
final class CorsSupport {
    private static final Set<String> ALLOWED = Set.of(
        "http://localhost:8300",
        "http://127.0.0.1:8300"
    );

    /**
     * Gère les requêtes preflight CORS.
     * @param ex Échange HTTP
     * @return true si preflight géré
     * @throws IOException En cas d'erreur
     */
    boolean handlePreflight(HttpExchange ex) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            return false;
        }
        String origin = origin(ex);
        if (!isAllowed(origin)) {
            return false;
        }
        applyCommon(ex, origin);
        String requested = ex.getRequestHeaders().getFirst("Access-Control-Request-Headers");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", requested != null ? requested : "Content-Type, Cookie");
        ex.sendResponseHeaders(204, -1);
        ex.close();
        return true;
    }

    /**
     * Applique les headers CORS à la réponse.
     * @param ex Échange HTTP
     */
    void apply(HttpExchange ex) {
        String origin = origin(ex);
        if (!isAllowed(origin)) {
            return;
        }
        applyCommon(ex, origin);
    }

    /**
     * Applique les headers CORS communs.
     * @param ex Échange HTTP
     * @param origin Origine de la requête
     */
    private void applyCommon(HttpExchange ex, String origin) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
        ex.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        ex.getResponseHeaders().add("Vary", "Origin");
    }

    /**
     * Extrait l'origine de la requête.
     * @param ex Échange HTTP
     * @return Origine ou null
     */
    private String origin(HttpExchange ex) {
        return ex.getRequestHeaders().getFirst("Origin");
    }

    /**
     * Vérifie si l'origine est autorisée.
     * @param origin Origine à vérifier
     * @return true si autorisée
     */
    private boolean isAllowed(String origin) {
        return origin != null && ALLOWED.contains(origin);
    }
}
