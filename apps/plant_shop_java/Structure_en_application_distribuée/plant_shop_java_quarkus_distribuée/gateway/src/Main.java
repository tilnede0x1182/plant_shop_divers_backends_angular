import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import util.EnvLoader;

final class GatewayRuntime {

    private final GatewayConfig config;
    private final HttpClient http;

    private GatewayRuntime(GatewayConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    static GatewayRuntime create() {
        GatewayConfig config = new GatewayConfig();
        HttpClient http = HttpClient.newBuilder().build();
        return new GatewayRuntime(config, http);
    }

    void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/api", new GatewayHandler(config, http));
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(16));
        server.start();
        System.out.printf("🚪 Gateway en écoute sur http://localhost:%d/api%n", config.port());
    }
}

final class GatewayConfig {

    int port() {
        return Integer.parseInt(EnvLoader.get("SERVER_ADDRESS", "4100"));
    }

    String serviceUrl(String service) {
        String host = EnvLoader.get("SERVICE_HOST", "http://localhost");
        return switch (service) {
            case "auth" -> host + ":" + EnvLoader.get("AUTH_SERVICE_PORT", "6101");
            case "catalog" -> host + ":" + EnvLoader.get("CATALOG_SERVICE_PORT", "6102");
            case "order" -> host + ":" + EnvLoader.get("ORDER_SERVICE_PORT", "6103");
            case "user" -> host + ":" + EnvLoader.get("USER_SERVICE_PORT", "6104");
            default -> throw new IllegalArgumentException("Service inconnu: " + service);
        };
    }
}
