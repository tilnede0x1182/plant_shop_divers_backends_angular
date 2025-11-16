import controller.OrderController;
import util.AuthMiddleware;
import util.ServiceRuntime;

public final class OrderService {

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
