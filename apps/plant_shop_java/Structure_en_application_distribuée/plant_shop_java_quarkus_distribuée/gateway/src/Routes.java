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
import java.util.Set;

final class GatewayHandler implements HttpHandler {

    private final GatewayConfig config;
    private final HttpClient http;
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
        "http://localhost:8300",
        "http://127.0.0.1:8300",
        "http://localhost:4200",
        "http://127.0.0.1:4200"
    );

    GatewayHandler(GatewayConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if (isCorsPreflight(ex)) {
                respondPreflight(ex);
                return;
            }
            forward(ex);
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, "{\"error\":\"Gateway failure\"}");
        }
    }

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

        RouteTarget target = RouteTarget.resolve(targetPath, ex.getRequestMethod());
        if (target == null) {
            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
            return;
        }

        SessionContext session = SessionContext.anonymous();
        boolean needsSession = !"auth".equals(target.service()) && (target.requiresAuth() || target.requiresAdmin());
        if (needsSession) {
            session = resolveSession(ex);
            if (!session.authenticated()) {
                sendJson(ex, 401, "{\"error\":\"Authentification requise\"}");
                return;
            }
            if (target.requiresAdmin() && !session.admin()) {
                sendJson(ex, 403, "{\"error\":\"Accès administrateur requis\"}");
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

        applyCorsHeaders(ex);
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

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return SessionContext.anonymous();
        }

        JSONObject json = new JSONObject(response.body());
        return new SessionContext(true, json.getInt("id"), json.optBoolean("admin", false));
    }

    private boolean isCorsPreflight(HttpExchange ex) {
        return "OPTIONS".equalsIgnoreCase(ex.getRequestMethod())
            && ex.getRequestHeaders().getFirst("Origin") != null;
    }

    private void respondPreflight(HttpExchange ex) throws IOException {
        applyCorsHeaders(ex);
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Cookie");
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    private void applyCorsHeaders(HttpExchange ex) {
        String origin = ex.getRequestHeaders().getFirst("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            ex.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
            ex.getResponseHeaders().set("Vary", "Origin");
        }
    }

private void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        applyCorsHeaders(ex);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }
}

record RouteTarget(String service, String path, boolean requiresAuth, boolean requiresAdmin) {
    static RouteTarget resolve(String path, String method) {
        if (path.startsWith("/auth")) {
            boolean needsAuth = path.startsWith("/auth/me")
                || path.startsWith("/auth/logout")
                || path.startsWith("/auth/_session");
            return new RouteTarget("auth", path, needsAuth, false);
        }
        if (path.startsWith("/admin/plants")) {
            return new RouteTarget("catalog", path, true, true);
        }
        if (path.startsWith("/plants")) {
            boolean adminOnly = !"GET".equalsIgnoreCase(method);
            return new RouteTarget("catalog", path, adminOnly, adminOnly);
        }
        if (path.startsWith("/orders")) {
            boolean admin = "PATCH".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method);
            return new RouteTarget("order", path, true, admin);
        }
        if (path.startsWith("/admin/orders")) {
            return new RouteTarget("order", path, true, true);
        }
        if (path.startsWith("/admin/users")) {
            return new RouteTarget("user", path, true, true);
        }
        if (path.equals("/users") || path.startsWith("/users?")) {
            return new RouteTarget("user", path, true, true);
        }
        if (path.startsWith("/users/")) {
            return new RouteTarget("user", path, true, false);
        }
        return null;
    }
}

record SessionContext(boolean authenticated, int userId, boolean admin) {
    static SessionContext anonymous() {
        return new SessionContext(false, -1, false);
    }
}
