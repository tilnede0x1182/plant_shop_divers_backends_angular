package util;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class ServiceRuntime {

    @FunctionalInterface
    public interface RouteRegistrar {
        void configure(Javalin app, Connection db, Map<String, String> env) throws Exception;
    }

    public record ServiceDescriptor(String serviceName, String portKey, int defaultPort) {
    }

    private ServiceRuntime() {
    }

    public static ServiceDescriptor descriptor(String serviceName, String portKey, int defaultPort) {
        return new ServiceDescriptor(serviceName, portKey, defaultPort);
    }

    public static void start(ServiceDescriptor descriptor, RouteRegistrar registrar) throws Exception {
        Map<String, String> env = loadEnv();
        Connection connection = connect(env);

        Javalin app = null;
        boolean success = false;
        try {
            app = Javalin.create(ServiceRuntime::configureJavalin);
            registrar.configure(app, connection, env);
            int port = resolvePort(descriptor, env);
            app.start(port);
            System.out.printf("🚀 %s en écoute sur http://localhost:%d%n", descriptor.serviceName(), port);
            success = true;
        } finally {
            if (!success) {
                safeClose(connection);
                if (app != null) {
                    app.stop();
                }
            } else {
                Connection conn = connection;
                Javalin server = app;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    safeClose(conn);
                    if (server != null) {
                        server.stop();
                    }
                }));
            }
        }
    }

    private static void configureJavalin(JavalinConfig config) {
        config.jsonMapper(new JavalinJsonMapper());
        config.http.defaultContentType = "application/json; charset=utf-8";
        config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
            rule.allowCredentials = true;
            rule.allowHost("http://localhost:4200");
            rule.allowHost("http://127.0.0.1:4200");
            rule.allowHost("http://localhost:4100");
            rule.allowHost("http://localhost:5173");
        }));
    }

    private static Map<String, String> loadEnv() throws IOException {
        Map<String, String> values = new HashMap<>(System.getenv());
        Path cwd = Path.of("").toAbsolutePath();
        readEnv(cwd.resolve("../config/.env"), values);
        readEnv(cwd.resolve("config/.env"), values);
        readEnv(cwd.resolve(".env"), values);
        return values;
    }

    private static void readEnv(Path path, Map<String, String> values) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (!key.isEmpty()) {
                    values.put(key, value);
                }
            }
        }
    }

    private static Connection connect(Map<String, String> env) throws SQLException {
        String url = env.get("DATABASE_URL");
        String user = env.getOrDefault("DATABASE_USER", "");
        String pass = env.getOrDefault("DATABASE_PASS", "");
        if (url == null) {
            throw new IllegalStateException("DATABASE_URL manquant");
        }
        return DriverManager.getConnection(url, user, pass);
    }

    private static int resolvePort(ServiceDescriptor descriptor, Map<String, String> env) {
        String raw = env.getOrDefault(descriptor.portKey(), env.get("SERVER_ADDRESS"));
        if (raw == null) {
            return descriptor.defaultPort();
        }
        return parsePort(raw, descriptor.defaultPort());
    }

    private static int parsePort(String raw, int fallback) {
        try {
            if (raw.contains(":")) {
                String[] parts = raw.split(":");
                return Integer.parseInt(parts[parts.length - 1]);
            }
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static void safeClose(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
