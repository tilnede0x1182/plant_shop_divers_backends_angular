import controller.ApplicationController;
import util.ServiceRuntime;

public final class UserService {

    public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("user-service", "USER_SERVICE_PORT", 6104),
            (db, env) -> {
                ApplicationController controller = new ApplicationController(db);
                return controller.getRoutes();
            }
        );
    }
}
