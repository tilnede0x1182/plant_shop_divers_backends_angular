package util;

import io.micronaut.runtime.Micronaut;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

public final class ServiceLauncher {

    private ServiceLauncher() {}

    public static void run(String appName, String portKey, int defaultPort, String... args) {
        Map<String, String> env = EnvLoader.load();
        int port = parsePort(env.getOrDefault(portKey, String.valueOf(defaultPort)));
        ensurePortAvailable(port, appName);

        Micronaut.build(args)
            .banner(false)
            .properties(Map.of(
                "micronaut.application.name", appName,
                "micronaut.server.port", String.valueOf(port)
            ))
            .start();

        System.out.printf("🚀 %s démarré sur http://localhost:%d%n", appName, port);
    }

    private static int parsePort(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        String value = raw.contains(":") ? raw.substring(raw.indexOf(':') + 1) : raw;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Port invalide : " + raw, e);
        }
    }

    private static void ensurePortAvailable(int port, String serviceName) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
        } catch (IOException e) {
            throw new IllegalStateException("Le port " + port + " est déjà utilisé pour " + serviceName, e);
        }
    }
}
