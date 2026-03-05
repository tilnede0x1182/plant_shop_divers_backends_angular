package launch;

import org.javalite.activeweb.AppContext;
import org.javalite.activeweb.Bootstrap;
import org.javalite.app_config.AppConfig;
import util.DatabaseFactory;

import java.util.Map;

/**
 * Bootstrap de l application ActiveWeb.
 * Initialise la configuration et l environnement.
 */
public final class AppBootstrap extends Bootstrap {

    /**
     * Initialise l application.
     * @param context AppContext Contexte de l application
     */
    @Override
    public void init(AppContext context) {
        AppConfig.setActiveEnv(System.getenv().getOrDefault("APP_ENV", "development"));
        Map<String, String> envValues = DatabaseFactory.loadEnv();
        envValues.forEach((key, value) -> System.setProperty(key, value));

        System.setProperty("activeweb.root_package", "app");
        System.setProperty("activeweb.encoding", "UTF-8");
    }
}
