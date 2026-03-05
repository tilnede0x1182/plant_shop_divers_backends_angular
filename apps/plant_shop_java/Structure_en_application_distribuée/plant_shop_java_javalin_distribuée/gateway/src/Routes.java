import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;
import util.Request;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Handler HTTP qui route les requêtes vers les services.
 */
final class GatewayHandler implements HttpHandler {

    private final GatewayConfig config;
    private final HttpClient http;

    /**
 * Constructeur.
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
 * Transmet la requête au service approprié.
 * @param ex Échange HTTP
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

        SessionContext session = resolveSession(ex);
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

        HttpResponse<byte[]> response = sendWithRetry(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

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

        HttpResponse<String> response = sendWithRetry(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return SessionContext.anonymous();
        }

        JSONObject json = new JSONObject(response.body());
        return new SessionContext(true, json.getInt("id"), json.optBoolean("admin", false));
    }

    private static void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    private <T> HttpResponse<T> sendWithRetry(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws Exception {
        IOException lastError = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                return http.send(request, handler);
            } catch (ConnectException | HttpTimeoutException e) {
                lastError = e;
                Thread.sleep(300);
            } catch (IOException e) {
                lastError = e;
                Thread.sleep(300);
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("Impossible d'appeler " + request.uri());
    }
}

record RouteTarget(String service, String path) {
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

record SessionContext(boolean authenticated, int userId, boolean admin) {
    static SessionContext anonymous() {
        return new SessionContext(false, -1, false);
    }
}
