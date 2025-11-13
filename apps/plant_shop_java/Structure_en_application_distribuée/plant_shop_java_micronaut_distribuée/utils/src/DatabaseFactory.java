package util;

import io.micronaut.context.annotation.Factory;
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
 * Provides the JDBC connection bean backed by the .env configuration.
 */
@Factory
public final class DatabaseFactory {

    private static final Path ENV_FILE = Path.of("config/.env");

    @Singleton
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

    private Map<String, String> loadEnv() throws IOException {
        if (!Files.exists(ENV_FILE)) return Map.of();
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
        return values;
    }
}
