package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Charge config/.env quelle que soit la position du service (racine ou dossier spécifique)
 * et offre des accesseurs compatibles avec les overrides d'environnement.
 */
public final class EnvLoader {

    private static final List<Path> CANDIDATES = List.of(
        Path.of("config", ".env"),
        Path.of("..", "config", ".env"),
        Path.of("..", "..", "config", ".env")
    );

    private static volatile Map<String, String> cached;

    private EnvLoader() {}

    public static Map<String, String> load() {
        Map<String, String> snapshot = cached;
        if (snapshot == null) {
            synchronized (EnvLoader.class) {
                snapshot = cached;
                if (snapshot == null) {
                    cached = snapshot = readAll();
                }
            }
        }
        return snapshot;
    }

    public static String get(String key, String fallback) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return load().getOrDefault(key, fallback);
    }

    private static Map<String, String> readAll() {
        Map<String, String> values = new HashMap<>();
        for (Path candidate : CANDIDATES) {
            read(candidate, values);
        }
        if (values.isEmpty()) {
            throw new IllegalStateException("config/.env introuvable (candidats: " + CANDIDATES + ")");
        }
        return Map.copyOf(values);
    }

    private static void read(Path path, Map<String, String> values) {
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
                    if (!key.isEmpty() && !value.isEmpty()) {
                        values.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire " + path + ": " + e.getMessage(), e);
        }
    }
}
