package util;

import io.micronaut.runtime.Micronaut;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

/**
 * Lance un microservice Micronaut en chargeant les ports depuis config/.env et en vérifiant les conflits.
 */
public final class ServiceLauncher {

    /**
 * Constructeur privé - classe utilitaire.
 */
private ServiceLauncher() {}

    /**
 * Lance un microservice Micronaut.
 * @param serviceName Nom du service
 * @param portEnvKey Clé env du port
 * @param defaultPort Port par défaut
 * @param args Arguments CLI
 */
public static void run(String serviceName, String portEnvKey, int defaultPort, String... args) {
        Map<String, String> env = EnvLoader.load();
        int port = parsePort(env.getOrDefault(portEnvKey, String.valueOf(defaultPort)));
        ensurePortAvailable(port, serviceName);

        Micronaut.build(args)
            .banner(false)
            .properties(Map.of(
                "micronaut.application.name", serviceName,
                "micronaut.server.port", String.valueOf(port)
            ))
            .start();

        System.out.printf("🚀 %s démarré sur http://localhost:%d%n", serviceName, port);
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
