package util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.RequestScope;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@Configuration
/** Factory de configuration de la base de donnees */
public class DatabaseFactory {

    private static final int DEFAULT_POOL_MIN = 1;
    private static final int DEFAULT_POOL_MAX = 6;

    @Bean(destroyMethod = "close")
    /** Cree et configure le pool de connexions HikariCP */
    public DataSource dataSource() {
        Map<String, String> env = EnvLoader.load();
        String url = env.get("DATABASE_URL");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASS");

        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("DATABASE_URL, DATABASE_USER et DATABASE_PASS doivent être définis dans config/.env");
        }

        int min = parseInt(env.get("DB_POOL_MIN"), DEFAULT_POOL_MIN);
        int max = parseInt(env.get("DB_POOL_MAX"), DEFAULT_POOL_MAX);
        if (max < 1) {
            max = DEFAULT_POOL_MAX;
        }
        if (min < 0 || min > max) {
            min = Math.min(DEFAULT_POOL_MIN, max);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setPoolName("plant-shop-spring-microservices");
        config.setMinimumIdle(min);
        config.setMaximumPoolSize(max);
        config.setDriverClassName("org.postgresql.Driver");
        config.setInitializationFailTimeout(-1);
        config.setConnectionTimeout(5000);
        return new HikariDataSource(config);
    }

    @Bean(destroyMethod = "close")
    @RequestScope
    /** Fournit une connexion DB pour la requete courante */
    public Connection connection(DataSource dataSource) throws SQLException {
        return dataSource.getConnection();
    }

    /** Parse un entier avec valeur par defaut */
    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
