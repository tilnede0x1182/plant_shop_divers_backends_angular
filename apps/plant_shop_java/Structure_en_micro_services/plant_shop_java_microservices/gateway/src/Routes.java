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
 * Handler HTTP pour le routage des requêtes.
 */
final class GatewayHandler implements HttpHandler {

    private final GatewayConfig config;
    private final HttpClient http;
    /**
	 * Constructeur.
	 * @param config Configuration du gateway
	 * @param http Client HTTP
	 */
	GatewayHandler(GatewayConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    /**
	 * Traite une requête HTTP entrante.
	 * @param ex Échange HTTP
	 * @throws IOException En cas d'erreur I/O
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
	 * Transmet une requête vers le service cible.
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

        ex.sendResponseHeaders(response.statusCode(), response.body().length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(response.body());
        }
    }

    /**
	 * Résout le contexte de session depuis les cookies.
	 * @param ex Échange HTTP
	 * @return Contexte de session
	 * @throws Exception En cas d'erreur
	 */
	private SessionContext resolveSession(HttpExchange ex) throws Exception {
        String sessionId = Request.extractSessionId(ex);
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
	 * Envoie une réponse JSON.
	 * @param ex Échange HTTP
	 * @param status Code HTTP
	 * @param body Corps de la réponse
	 * @throws IOException En cas d'erreur I/O
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
 * Record représentant la cible d'une route.
 * @param service Nom du service cible
 * @param path Chemin de la route
 */
record RouteTarget(String service, String path) {
    /**
	 * Résout la cible d'une route depuis un chemin.
	 * @param path Chemin de la requête
	 * @return RouteTarget ou null si non trouvé
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
 * Record représentant le contexte de session.
 * @param authenticated true si authentifié
 * @param userId ID de l'utilisateur
 * @param admin true si administrateur
 */
record SessionContext(boolean authenticated, int userId, boolean admin) {
    /**
	 * Crée un contexte de session anonyme.
	 * @return Contexte anonyme
	 */
	static SessionContext anonymous() {
        return new SessionContext(false, -1, false);
    }
}
