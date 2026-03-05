package launch;

import org.javalite.activeweb.AbstractControllerConfig;
import org.javalite.activeweb.AppContext;
import app.controllers.AppController;

/**
 * Configuration des contrôleurs ActiveWeb.
 */
public final class AppControllerConfig extends AbstractControllerConfig<AppController> {

    /**
     * Initialise la configuration des contrôleurs.
     * @param context AppContext Contexte de l application
     */
    @Override
    public void init(AppContext context) {
    }
}
