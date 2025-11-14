package util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
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
 * Fournit la connexion JDBC via CDI (Contexts and Dependency Injection).
 * Crée une nouvelle connexion pour chaque requête HTTP (@RequestScoped).
 */
@ApplicationScoped // Le Factory lui-même est un singleton
public final class DatabaseFactory {

    // Note: L'emplacement "config/.env" est utilisé par le Seed.java
    private static final Path ENV_FILE = Path.of("config", ".env");
    private static Map<String, String> envCache;

    /**
     * Charge le fichier .env une seule fois.
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
     * Méthode "Producer" qui fournit une connexion à la base de données.
     * @Produces indique à Quarkus que cette méthode crée un bean.
     * @RequestScoped indique que ce bean (la Connexion) doit être créé
     * une fois par requête HTTP et détruit à la fin de la requête.
     */
    @Produces
    @RequestScoped
    public Connection connection() throws SQLException, IOException {
        Map<String, String> env = loadEnv();
        String url = env.get("DATABASE_URL");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASS");

        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("DATABASE_URL, DATABASE_USER et DATABASE_PASS doivent être définis dans config/.env");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}
