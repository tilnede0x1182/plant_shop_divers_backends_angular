package launch;

import org.javalite.activeweb.AbstractControllerConfig;
import org.javalite.activeweb.AppContext;
import app.controllers.AppController;

public final class AppControllerConfig extends AbstractControllerConfig<AppController> {

    @Override
    public void init(AppContext context) {
        // Aucun filtre global spécifique pour le moment.
    }
}
