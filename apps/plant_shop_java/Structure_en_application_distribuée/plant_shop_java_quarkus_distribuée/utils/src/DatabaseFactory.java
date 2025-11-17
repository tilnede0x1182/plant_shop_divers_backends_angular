package util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * Fournit la connexion JDBC via CDI (Contexts and Dependency Injection).
 * Crée une nouvelle connexion pour chaque requête HTTP (@RequestScoped).
 */
@ApplicationScoped // Le Factory lui-même est un singleton
public final class DatabaseFactory {

    /**
     * Méthode "Producer" qui fournit une connexion à la base de données.
     * @Produces indique à Quarkus que cette méthode crée un bean.
     * @RequestScoped indique que ce bean (la Connexion) doit être créé
     * une fois par requête HTTP et détruit à la fin de la requête.
     */
    @Produces
    @RequestScoped
    public Connection connection() throws SQLException {
        Map<String, String> env = EnvLoader.load();
        String url = env.get("DATABASE_URL");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASS");

        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("DATABASE_URL, DATABASE_USER et DATABASE_PASS doivent être définis dans config/.env");
        }
        return DriverManager.getConnection(url, user, pass);
    }
}
