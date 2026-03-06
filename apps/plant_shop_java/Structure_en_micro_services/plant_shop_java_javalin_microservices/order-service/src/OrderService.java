import controller.ApplicationController;
import util.ServiceRuntime;

/**
 * Point d'entrée du microservice de gestion des commandes.
 */
public final class OrderService {

    /**
     * Lance le service de commandes sur le port configuré.
     * @param args Arguments CLI (non utilisés)
     * @throws Exception En cas d'erreur de démarrage
     */
    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("order-service", "ORDER_SERVICE_PORT", 6103),
            (db, env) -> {
                ApplicationController controller = new ApplicationController(db, env);
                return controller.getRoutes();
            }
        );
    }
}
