import controller.ApplicationController;
import util.ServiceRuntime;

public final class AuthService {

    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("auth-service", "AUTH_SERVICE_PORT", 6101),
            (app, db, env) -> {
                ApplicationController controller = new ApplicationController(db);
                app.routes(controller.getRoutes());
            }
        );
    }
}
