import controller.ApplicationController;
import util.ServiceRuntime;

public final class OrderService {

    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("order-service", "ORDER_SERVICE_PORT", 6103),
            (app, db, env) -> {
                ApplicationController controller = new ApplicationController(db, env);
                app.routes(controller.getRoutes());
            }
        );
    }
}
