package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge config/.env depuis le service courant ou le dossier parent.
 */
public final class EnvLoader {

    /**
 * Constructeur privé - classe utilitaire.
 */
private EnvLoader() {}

    public static Map<String, String> load() {
        Map<String, String> values = new HashMap<>();
        read(Path.of("config", ".env"), values);
        read(Path.of("..", "config", ".env"), values);
        if (values.isEmpty()) {
            throw new IllegalStateException("config/.env introuvable");
        }
        return values;
    }

    /**
     * Lit un fichier .env et ajoute les valeurs à la map.
     * @param path Path Chemin du fichier
     * @param values Map Map à remplir
     */
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
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Impossible de lire " + path + ": " + e.getMessage(), e);
        }
    }
}
