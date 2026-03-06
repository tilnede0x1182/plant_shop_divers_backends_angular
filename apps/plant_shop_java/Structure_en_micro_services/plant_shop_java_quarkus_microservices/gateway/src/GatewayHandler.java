import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import util.Request;

/**
 * Handler HTTP pour le routage des requetes vers les microservices.
 * Gere l'authentification et la propagation des headers.
 */
public final class GatewayHandler implements HttpHandler {

    private final GatewayConfig config;
    private final HttpClient http;

    /**
     * Constructeur du handler.
     *
     * @param config Configuration de la Gateway
     * @param http Client HTTP pour les appels aux services
     */
    public GatewayHandler(GatewayConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    /**
     * Traite une requete HTTP entrante.
     *
     * @param ex Echange HTTP
     * @throws IOException En cas d'erreur d'entree/sortie
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
     * Transmet la requete au microservice cible.
     *
     * @param ex Echange HTTP
     * @throws Exception En cas d'erreur lors du routage
     */
    private void forward(HttpExchange ex) throws Exception {
        URI uri = ex.getRequestURI();
        String path = uri.getPath();
        if (!path.startsWith("/api")) {
            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
            return;
        }

        String targetPath = path;
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            targetPath = targetPath + "?" + uri.getRawQuery();
        }

        RouteTarget target = RouteTarget.resolve(targetPath);
        if (target == null) {
            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
            return;
        }

        boolean sessionIntrospection = "auth".equals(target.service()) && target.path().startsWith("/api/auth/_session");
        SessionContext session = sessionIntrospection
            ? SessionContext.anonymous()
            : resolveSession(ex);

        boolean needAuth = config.requiresAuth(target.service(), ex.getRequestMethod(), target.path());
        if (needAuth && !session.authenticated()) {
            sendJson(ex, 401, "{\"error\":\"Authentification requise\"}");
            return;
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
     * Resout la session utilisateur via le service d'authentification.
     *
     * @param ex Echange HTTP contenant le cookie de session
     * @return Contexte de session (authentifie ou anonyme)
     * @throws Exception En cas d'erreur lors de la resolution
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
     * Envoie une reponse JSON.
     *
     * @param ex Echange HTTP
     * @param status Code de statut HTTP
     * @param body Corps de la reponse JSON
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
