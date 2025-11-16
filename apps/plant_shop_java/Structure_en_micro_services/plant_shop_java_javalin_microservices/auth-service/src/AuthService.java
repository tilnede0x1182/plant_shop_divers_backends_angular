import controller.ApplicationController;
import util.ServiceRuntime;

public final class AuthService {

    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("auth-service", "AUTH_SERVICE_PORT", 6101),
            (db, env) -> {
                ApplicationController controller = new ApplicationController(db);
                return controller.getRoutes();
            }
        );
    }
}
