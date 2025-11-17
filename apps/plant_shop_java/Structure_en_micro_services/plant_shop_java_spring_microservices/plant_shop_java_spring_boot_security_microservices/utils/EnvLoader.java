package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class EnvLoader {

    private static final Path[] CANDIDATES = new Path[] {
        Path.of("..", "config", ".env"),
        Path.of("config", ".env"),
        Path.of("..", ".env"),
        Path.of(".env")
    };

    private static volatile Map<String, String> cache;

    private EnvLoader() {}

    public static Map<String, String> load() {
        Map<String, String> local = cache;
        if (local != null) {
            return local;
        }
        synchronized (EnvLoader.class) {
            if (cache == null) {
                cache = Collections.unmodifiableMap(readEnv());
            }
        }
        return cache;
    }

    private static Map<String, String> readEnv() {
        for (Path candidate : CANDIDATES) {
            if (Files.exists(candidate)) {
                return parse(candidate);
            }
        }
        return Map.of();
    }

    private static Map<String, String> parse(Path file) {
        Map<String, String> values = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️  Impossible de lire " + file + " : " + e.getMessage());
        }
        return values;
    }
}
