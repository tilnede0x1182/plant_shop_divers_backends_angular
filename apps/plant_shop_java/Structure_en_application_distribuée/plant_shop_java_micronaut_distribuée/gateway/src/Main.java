import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class GatewayRuntime {

    private final GatewayConfig config;
    private final HttpClient http;
    private final SessionRegistry sessions;

    private GatewayRuntime(GatewayConfig config, HttpClient http, SessionRegistry sessions) {
        this.config = config;
        this.http = http;
        this.sessions = sessions;
    }

    static GatewayRuntime create() throws Exception {
        GatewayConfig config = GatewayConfig.load();
        HttpClient http = HttpClient.newBuilder().build();
        return new GatewayRuntime(config, http, new SessionRegistry());
    }

    void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/api", new GatewayHandler(config, http, sessions));
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(16));
        server.start();
        System.out.printf("🚪 Gateway en écoute sur http://localhost:%d/api%n", config.port());
    }
}

final class GatewayConfig {
    private final Map<String, String> values;

    private GatewayConfig(Map<String, String> values) {
        this.values = values;
    }

    static GatewayConfig load() throws IOException {
        Map<String, String> values = new HashMap<>();
        readEnv(Path.of("../config/.env"), values);
        readEnv(Path.of(".env"), values);
        return new GatewayConfig(values);
    }

    int port() {
        return Integer.parseInt(values.getOrDefault("SERVER_ADDRESS", "4100"));
    }

    String serviceUrl(String service) {
        String host = values.getOrDefault("SERVICE_HOST", "http://localhost");
        return switch (service) {
            case "auth" -> host + ":" + values.getOrDefault("AUTH_SERVICE_PORT", "6101");
            case "catalog" -> host + ":" + values.getOrDefault("CATALOG_SERVICE_PORT", "6102");
            case "order" -> host + ":" + values.getOrDefault("ORDER_SERVICE_PORT", "6103");
            case "user" -> host + ":" + values.getOrDefault("USER_SERVICE_PORT", "6104");
            default -> throw new IllegalArgumentException("Service inconnu: " + service);
        };
    }

    boolean requiresAuth(String service, String method, String path) {
        if ("auth".equals(service)) {
            return false;
        }
        if ("catalog".equals(service) && "GET".equals(method) && path.startsWith("/plants")
            && !path.startsWith("/admin")) {
            return false;
        }
        return true;
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
}
