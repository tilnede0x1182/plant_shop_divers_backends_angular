import controller.OrderController;
import util.AuthMiddleware;
import util.ServiceRuntime;

/**
 * Service de gestion des commandes Javalin.
 */
public final class OrderService {

    /**
	 * Point d'entrée du service.
	 * @param args Arguments de ligne de commande
	 */
	public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("order-service", "ORDER_SERVICE_PORT", 6103),
            (app, db) -> {
                OrderController orderController = new OrderController(db);

                app.get("/orders", AuthMiddleware.requireUser(orderController::list));
                app.post("/orders", AuthMiddleware.requireUser(orderController::create));
                app.patch("/orders/{id}", AuthMiddleware.requireAdmin(orderController::patch));
                app.delete("/orders/{id}", AuthMiddleware.requireAdmin(orderController::destroy));
            }
        );
    }
}
