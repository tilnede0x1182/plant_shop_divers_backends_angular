package util;

import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.http.scope.RequestScope;
import jakarta.inject.Singleton;
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

/**
 * Factory Micronaut qui fournit la connexion JDBC.
 * Une nouvelle connexion est créée pour chaque requête HTTP (@RequestScope).
 */
@Factory
public final class DatabaseFactory {

    private static final Path ENV_FILE = Path.of("config/.env");
    private static Map<String, String> envCache;

    /**
     * Charge les variables d'environnement depuis le fichier .env.
     * @return Map contenant les paires clé-valeur du fichier .env
     * @throws IOException Si le fichier ne peut pas être lu
     */
    private synchronized Map<String, String> loadEnv() throws IOException {
        if (envCache == null) {
            if (!Files.exists(ENV_FILE)) {
                envCache = Map.of();
                return envCache;
            }
            Map<String, String> values = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(ENV_FILE.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        values.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                    }
                }
            }
            envCache = values;
        }
        return envCache;
    }

    /**
     * Produit une connexion par requête HTTP.
     * @RequestScope indique que ce bean est créé une fois par requête et détruit à la fin.
     */
    @RequestScope
    public Connection connection() throws SQLException, IOException {
        Map<String, String> env = loadEnv();
        String url = env.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost/plant_shop_java_micronaut");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASS");
        if (user == null || pass == null) {
            throw new IllegalStateException("DATABASE_USER et DATABASE_PASS doivent être définis dans config/.env");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}
