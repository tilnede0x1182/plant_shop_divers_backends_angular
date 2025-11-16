import controller.ApplicationController;
import util.ServiceRuntime;

public final class CatalogService {

    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("catalog-service", "CATALOG_SERVICE_PORT", 6102),
            (app, db, env) -> {
                ApplicationController controller = new ApplicationController(db);
                app.routes(controller.getRoutes());
            }
        );
    }
}
