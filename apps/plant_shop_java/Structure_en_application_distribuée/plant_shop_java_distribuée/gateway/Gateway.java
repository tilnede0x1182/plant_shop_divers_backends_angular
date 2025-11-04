import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONObject;
import util.Request;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Gateway {

    private static final HttpClient HTTP = HttpClient.newBuilder().build();
    private static Map<String, String> cfg;

    public static void main(String[] args) throws Exception {
        cfg = loadEnv();
        int port = Integer.parseInt(cfg.getOrDefault("SERVER_ADDRESS", "4100"));

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", new GatewayHandler());
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(16));
        server.start();
        System.out.printf("🚪 Gateway en écoute sur http://localhost:%d/api%n", port);
    }

    private static Map<String, String> loadEnv() throws IOException {
        Map<String, String> values = new HashMap<>();
        readEnv(Path.of("../config/.env"), values);
        readEnv(Path.of(".env"), values);
        return values;
    }

    private static void readEnv(Path path, Map<String, String> values) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
            }
        }
    }

    private static String serviceUrl(String service) {
        String host = cfg.getOrDefault("SERVICE_HOST", "http://localhost");
        return switch (service) {
            case "auth" -> host + ":" + cfg.getOrDefault("AUTH_SERVICE_PORT", "6101");
            case "catalog" -> host + ":" + cfg.getOrDefault("CATALOG_SERVICE_PORT", "6102");
            case "order" -> host + ":" + cfg.getOrDefault("ORDER_SERVICE_PORT", "6103");
            case "user" -> host + ":" + cfg.getOrDefault("USER_SERVICE_PORT", "6104");
            default -> throw new IllegalArgumentException("Service inconnu: " + service);
        };
    }

    private record RouteTarget(String service, String path) {}

    private static RouteTarget resolveTarget(String path) {
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

    private static boolean requiresAuth(String service, String method, String path) {
        if ("auth".equals(service)) {
            return false;
        }
        if ("catalog".equals(service) && "GET".equals(method) && path.startsWith("/plants")
            && !path.startsWith("/admin")) {
            return false;
        }
        return true;
    }

    private static SessionContext resolveSession(HttpExchange ex) throws Exception {
        String sessionId = Request.extractSessionId(ex);
        if (sessionId == null) {
            return SessionContext.anonymous();
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(serviceUrl("auth") + "/auth/_session"))
            .header("Cookie", "session_id=" + sessionId)
            .GET()
            .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return SessionContext.anonymous();
        }

        JSONObject json = new JSONObject(response.body());
        return new SessionContext(true, json.getInt("id"), json.optBoolean("admin", false));
    }

    private record SessionContext(boolean authenticated, int userId, boolean admin) {
        static SessionContext anonymous() {
            return new SessionContext(false, -1, false);
        }
    }

    private static final class GatewayHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            try {
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

            RouteTarget target = resolveTarget(targetPath);
            if (target == null) {
                sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
                return;
            }

            SessionContext session = SessionContext.anonymous();
            boolean needAuth = requiresAuth(target.service(), ex.getRequestMethod(), target.path());
            if (!"auth".equals(target.service())) {
                session = resolveSession(ex);
                if (needAuth && !session.authenticated()) {
                    sendJson(ex, 401, "{\"error\":\"Authentification requise\"}");
                    return;
                }
            }

            byte[] body = ex.getRequestBody().readAllBytes();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(serviceUrl(target.service()) + target.path()))
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

            HttpResponse<byte[]> response = HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

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
}
