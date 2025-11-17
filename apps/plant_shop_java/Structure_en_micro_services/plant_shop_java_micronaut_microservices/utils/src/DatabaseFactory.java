package util;

import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * Provides the JDBC connection bean backed by the .env configuration.
 */
@Factory
public final class DatabaseFactory {

    @Singleton
    public Connection connection() throws SQLException, IOException {
        Map<String, String> env = EnvLoader.load();
        String url = env.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost/plant_shop_java_micronaut");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASS");
        if (user == null || pass == null) {
            throw new IllegalStateException("DATABASE_USER et DATABASE_PASS doivent être définis dans config/.env");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}
