package launch;

import org.javalite.activeweb.AppContext;
import org.javalite.activeweb.Bootstrap;
import org.javalite.app_config.AppConfig;
import util.DatabaseFactory;

import java.util.Map;

public final class AppBootstrap extends Bootstrap {

    @Override
    public void init(AppContext context) {
        AppConfig.setActiveEnv(System.getenv().getOrDefault("APP_ENV", "development"));
        Map<String, String> envValues = DatabaseFactory.loadEnv();
        envValues.forEach((key, value) -> System.setProperty(key, value));

        System.setProperty("activeweb.root_package", "controllers");
        System.setProperty("activeweb.encoding", "UTF-8");
    }
}
