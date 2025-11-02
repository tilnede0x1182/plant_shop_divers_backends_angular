package utils;

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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class DatabaseFactory {

    private static final Path ENV_FILE = Path.of("config", ".env");
    private static Map<String, String> envCache;

    // Logique de chargement du .env (identique à votre DatabaseFactory)
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
     * Fournit une connexion JDBC unique par requête HTTP.
     * @Bean indique à Spring de gérer cet objet.
     * @RequestScope garantit un bean par requête.
     * La connexion sera automatiquement fermée à la fin de la requête.
     */
    @Bean
    @RequestScope
    public Connection connection() throws SQLException, IOException {
        Map<String, String> env = loadEnv();
        String url = env.get("DATABASE_URL");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASS");

        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("DATABASE_URL, DATABASE_USER et DATABASE_PASS doivent être définis dans config/.env");
        }

        // Spring gérera le .close() de cette connexion à la fin de la requête
        return DriverManager.getConnection(url, user, pass);
    }
}
