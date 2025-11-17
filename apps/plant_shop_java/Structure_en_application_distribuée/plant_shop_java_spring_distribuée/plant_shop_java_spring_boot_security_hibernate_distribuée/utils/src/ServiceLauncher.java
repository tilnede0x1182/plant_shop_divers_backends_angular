package util;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

public final class ServiceLauncher {

    private ServiceLauncher() {}

    public static void launch(Class<?> source,
                              String envPortKey,
                              int defaultPort,
                              String serviceName,
                              String defaultDbName,
                              String[] args) {
        Map<String, String> env = EnvLoader.load();
        String rawPort = env.getOrDefault(envPortKey,
            env.getOrDefault("SERVER_ADDRESS",
                env.getOrDefault("SERVER_ADRRESS", String.valueOf(defaultPort))));
        int port = parsePort(rawPort, defaultPort);
        if (!isPortAvailable(port)) {
            System.err.printf("❌ Port %d déjà utilisé, impossible de démarrer %s.%n", port, serviceName);
            return;
        }

        Map<String, Object> defaults = new HashMap<>();
        defaults.put("server.port", port);
        applyDataSource(defaults, env, defaultDbName);

        SpringApplication app = new SpringApplication(source);
        app.setDefaultProperties(defaults);
        app.addListeners((ApplicationListener<ApplicationReadyEvent>) event ->
            System.out.printf("✅ %s prêt sur http://localhost:%d%n", serviceName, port)
        );

        System.out.printf("🚀 Démarrage de %s sur http://localhost:%d%n", serviceName, port);
        app.run(args);
    }

    private static void applyDataSource(Map<String, Object> defaults,
                                        Map<String, String> env,
                                        String defaultDbName) {
        String jdbcUrl = env.getOrDefault("DATABASE_URL",
            "jdbc:postgresql://localhost:5432/" + defaultDbName);
        String username = env.getOrDefault("DATABASE_USER", "postgres");
        String password = env.getOrDefault("DATABASE_PASS", "postgres");
        String min = env.getOrDefault("DB_POOL_MIN", "1");
        String max = env.getOrDefault("DB_POOL_MAX", "5");

        defaults.put("spring.datasource.url", jdbcUrl);
        defaults.put("spring.datasource.username", username);
        defaults.put("spring.datasource.password", password);
        defaults.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        defaults.put("spring.datasource.hikari.minimum-idle", min);
        defaults.put("spring.datasource.hikari.maximum-pool-size", max);
        defaults.put("spring.jpa.open-in-view", "false");
        defaults.put("spring.jpa.hibernate.ddl-auto", "validate");
        defaults.put("spring.jpa.properties.hibernate.jdbc.time_zone", "UTC");
    }

    private static int parsePort(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String candidate = raw.contains(":") ? raw.substring(raw.lastIndexOf(':') + 1) : raw;
        try {
            return Integer.parseInt(candidate.trim());
        } catch (NumberFormatException e) {
            System.err.printf("⚠️  Port invalide (%s), fallback %d.%n", raw, fallback);
            return fallback;
        }
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
