package util;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;

/**
 * Fournit un pool de connexions JDBC via Agroal et expose des connexions par requête.
 * Le pool limite le nombre de connexions physiques ouvertes simultanément.
 */
@ApplicationScoped // Le Factory lui-même est un singleton
public final class DatabaseFactory {

    private static final int DEFAULT_POOL_MIN = 1;
    private static final int DEFAULT_POOL_MAX = 4;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    @Produces
    @ApplicationScoped
    public AgroalDataSource dataSource() throws SQLException {
        Map<String, String> env = EnvLoader.load();
        String url = env.get("DATABASE_URL");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASS");

        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("DATABASE_URL, DATABASE_USER et DATABASE_PASS doivent être définis dans config/.env");
        }

        int minSize = parseInt(env.get("DB_POOL_MIN"), DEFAULT_POOL_MIN);
        int maxSize = parseInt(env.get("DB_POOL_MAX"), DEFAULT_POOL_MAX);
        if (maxSize < 1) {
            throw new IllegalStateException("DB_POOL_MAX doit être ≥ 1");
        }
        if (minSize < 0 || minSize > maxSize) {
            minSize = Math.min(DEFAULT_POOL_MIN, maxSize);
        }

        AgroalDataSourceConfigurationSupplier cfg = new AgroalDataSourceConfigurationSupplier();
        cfg.connectionPoolConfiguration(configurePool(url, user, pass, minSize, maxSize));
        return AgroalDataSource.from(cfg);
    }

    @Produces
    @RequestScoped
    public Connection connection(AgroalDataSource dataSource) throws SQLException {
        return dataSource.getConnection();
    }

    public void closeConnection(@Disposes Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close(); // Retourne la connexion au pool
        } catch (SQLException e) {
            System.err.println("⚠️  Impossible de rendre la connexion au pool: " + e.getMessage());
        }
    }

    public void closeDataSource(@Disposes AgroalDataSource dataSource) {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception e) {
                System.err.println("⚠️  Impossible de fermer le pool de connexions: " + e.getMessage());
            }
        }
    }

    private static AgroalConnectionPoolConfigurationSupplier configurePool(
        String url,
        String user,
        String pass,
        int minSize,
        int maxSize
    ) {
        AgroalConnectionPoolConfigurationSupplier pool = new AgroalConnectionPoolConfigurationSupplier();
        pool.minSize(minSize);
        pool.initialSize(minSize);
        pool.maxSize(maxSize);
        pool.acquisitionTimeout(DEFAULT_TIMEOUT);
        pool.connectionFactoryConfiguration(configureFactory(url, user, pass));
        return pool;
    }

    private static AgroalConnectionFactoryConfigurationSupplier configureFactory(
        String url,
        String user,
        String pass
    ) {
        AgroalConnectionFactoryConfigurationSupplier factory = new AgroalConnectionFactoryConfigurationSupplier();
        factory.jdbcUrl(url);
        factory.principal(new NamePrincipal(user));
        factory.credential(new SimplePassword(pass));
        factory.connectionProviderClassName("org.postgresql.Driver");
        return factory;
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
