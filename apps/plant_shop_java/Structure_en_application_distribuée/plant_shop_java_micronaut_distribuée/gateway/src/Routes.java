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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler HTTP qui route les requêtes vers les services.
 */
final class GatewayHandler implements HttpHandler {

    private final GatewayConfig config;
    private final HttpClient http;

    private final SessionRegistry sessions;
    private final CorsSupport cors = new CorsSupport();

    /**
 * Constructeur.
 *
 * @param config GatewayConfig Configuration de la gateway
 * @param http HttpClient Client HTTP
 * @param sessions SessionRegistry Registre des sessions
 */
GatewayHandler(GatewayConfig config, HttpClient http, SessionRegistry sessions) {
        this.config = config;
        this.http = http;
        this.sessions = sessions;
    }

    /**
 * Gère une requête HTTP entrante.
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
 * Transmet la requête au service approprié.
 *
 * @param ex HttpExchange L'échange HTTP à transmettre
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
            session = resolveSession(Request.extractSessionId(ex));
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
        byte[] responseBody = response.body();

        mirrorResponseHeaders(ex, response);
        handleSessionSideEffects(ex, target, response, responseBody);
        cors.apply(ex);

        ex.sendResponseHeaders(response.statusCode(), responseBody.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(responseBody);
        }
    }

    /**
	 * Résout la session depuis l'ID.
	 * @param sessionId ID de session
	 * @return Contexte de session
	 */
	private SessionContext resolveSession(String sessionId) throws Exception {
        if (sessionId == null) {
            return SessionContext.anonymous();
        }
        SessionContext cached = sessions.get(sessionId);
        if (cached != null) {
            return cached;
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
        SessionContext context = SessionContext.authenticated(json.getInt("id"), json.optBoolean("admin", false));
        sessions.put(sessionId, context);
        return context;
    }

    /**
	 * Copie les headers de réponse vers l'échange.
	 * @param ex Échange HTTP
	 * @param response Réponse du service
	 */
	private void mirrorResponseHeaders(HttpExchange ex, HttpResponse<byte[]> response) {
        ex.getResponseHeaders().set("Content-Type",
            response.headers().firstValue("Content-Type").orElse("application/json"));
        response.headers().map().forEach((key, values) -> {
            if ("set-cookie".equalsIgnoreCase(key)) {
                for (String value : values) {
                    ex.getResponseHeaders().add("Set-Cookie", value);
                }
            }
        });
    }

    /**
	 * Gère les effets de bord sur la session.
	 * @param ex Échange HTTP
	 * @param target Cible de routage
	 * @param response Réponse du service
	 * @param body Corps de la réponse
	 */
	private void handleSessionSideEffects(HttpExchange ex, RouteTarget target, HttpResponse<byte[]> response, byte[] body) {
        if (!"auth".equals(target.service())) {
            return;
        }
        if (response.statusCode() == 201 && target.path().startsWith("/auth/login")) {
            registerSessionFromLogin(response, body);
            return;
        }
        if (response.statusCode() <= 204 && target.path().startsWith("/auth/logout")) {
            Optional.ofNullable(Request.extractSessionId(ex)).ifPresent(sessions::remove);
        }
    }

    /**
	 * Enregistre la session après login.
	 * @param response Réponse du service auth
	 * @param body Corps de la réponse
	 */
	private void registerSessionFromLogin(HttpResponse<byte[]> response, byte[] body) {
        String sessionId = extractSessionId(response.headers().allValues("set-cookie"));
        if (sessionId == null) {
            return;
        }
        try {
            JSONObject json = new JSONObject(new String(body, StandardCharsets.UTF_8));
            sessions.put(sessionId, SessionContext.authenticated(json.getInt("id"), json.optBoolean("admin", false)));
        } catch (Exception e) {
            System.err.println("⚠️  Impossible d'enregistrer la session gateway: " + e.getMessage());
        }
    }

    /**
	 * Extrait l'ID de session des cookies Set-Cookie.
	 * @param setCookies Liste des headers Set-Cookie
	 * @return ID de session ou null
	 */
	private String extractSessionId(List<String> setCookies) {
        for (String header : setCookies) {
            String trimmed = header.trim();
            if (trimmed.startsWith("session_id=")) {
                int end = trimmed.indexOf(';');
                return end > 0 ? trimmed.substring("session_id=".length(), end) : trimmed.substring("session_id=".length());
            }
        }
        return null;
    }

    /**
	 * Envoie une réponse JSON.
	 * @param ex Échange HTTP
	 * @param status Code de statut
	 * @param body Corps JSON
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
 * Cible de routage (service et chemin).
 * @param service Nom du service cible
 * @param path Chemin vers le service
 */
record RouteTarget(String service, String path) {
    /**
	 * Résout la cible depuis un chemin.
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
 * @param admin true si administrateur
 */
record SessionContext(boolean authenticated, int userId, boolean admin) {
    /**
	 * Crée un contexte anonyme.
	 * @return Contexte anonyme
	 */
	static SessionContext anonymous() {
        return new SessionContext(false, -1, false);
    }

    /**
	 * Crée un contexte authentifié.
	 * @param userId ID de l'utilisateur
	 * @param admin true si admin
	 * @return Contexte authentifié
	 */
	static SessionContext authenticated(int userId, boolean admin) {
        return new SessionContext(true, userId, admin);
    }
}

/**
 * Registre des sessions actives.
 */
final class SessionRegistry {
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    /**
	 * Récupère une session.
	 * @param sessionId ID de session
	 * @return Contexte ou null
	 */
	SessionContext get(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
	 * Enregistre une session.
	 * @param sessionId ID de session
	 * @param context Contexte à enregistrer
	 */
	void put(String sessionId, SessionContext context) {
        sessions.put(sessionId, context);
    }

    /**
	 * Supprime une session.
	 * @param sessionId ID de session
	 */
	void remove(String sessionId) {
        sessions.remove(sessionId);
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
	 * Gère les requêtes preflight OPTIONS.
	 * @param ex Échange HTTP
	 * @return true si preflight traité
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
	 * @return Valeur du header Origin
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
