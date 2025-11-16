import controller.ApplicationController;
import util.ServiceRuntime;

public final class OrderService {

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
