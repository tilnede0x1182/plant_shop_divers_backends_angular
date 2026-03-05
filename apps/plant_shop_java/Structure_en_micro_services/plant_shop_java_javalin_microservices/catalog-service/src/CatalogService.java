import controller.ApplicationController;
import util.ServiceRuntime;

/**
 * Point d'entrée du service de catalogue.
 */
public final class CatalogService {

    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("catalog-service", "CATALOG_SERVICE_PORT", 6102),
            (db, env) -> {
                ApplicationController controller = new ApplicationController(db);
                return controller.getRoutes();
            }
        );
    }
}
