package util;

import org.javalite.activeweb.AbstractDBConfig;
import org.javalite.activeweb.AppContext;
import util.DatabaseFactory;

import java.util.Map;

/**
 * Configuration de la base de données pour ActiveWeb.
 */
public final class DbConfig extends AbstractDBConfig {

    @Override
    /**
     * Initialise la configuration de la base de données.
     * @param context AppContext Contexte de l'application
     */
    public void init(AppContext context) {
        Map<String, String> env = DatabaseFactory.loadEnv();
        String jdbc = env.getOrDefault("DATABASE_URL", DatabaseFactory.jdbcUrlOrDefault());
        String user = env.getOrDefault("DATABASE_USER", DatabaseFactory.dbUserOrDefault());
        String pass = env.getOrDefault("DATABASE_PASS", DatabaseFactory.dbPassOrDefault());

        environment("development").jdbc("org.postgresql.Driver", jdbc, user, pass);
        environment("test", true).jdbc("org.postgresql.Driver", jdbc, user, pass);
    }
}
