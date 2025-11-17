package util;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

public final class ServiceLauncher {

    private ServiceLauncher() {}

    public static void launch(Class<?> source,
                              String envPortKey,
                              int defaultPort,
                              String serviceName,
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

        System.setProperty("server.port", String.valueOf(port));
        SpringApplication app = new SpringApplication(source);
        app.addListeners((ApplicationListener<ApplicationReadyEvent>) event ->
            System.out.printf("✅ %s prêt sur http://localhost:%d%n", serviceName, port)
        );
        System.out.printf("🚀 Démarrage de %s sur http://localhost:%d%n", serviceName, port);
        app.run(args);
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
