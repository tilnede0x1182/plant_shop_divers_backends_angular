import controller.UserController;
import util.AuthMiddleware;
import util.ServiceRuntime;

/**
 * Service de gestion des utilisateurs Javalin.
 */
public final class UserService {

    /**
	 * Point d'entrée du service.
	 * @param args Arguments de ligne de commande
	 */
	public static void main(String[] args) throws Exception {
        ServiceRuntime.start(
            ServiceRuntime.descriptor("user-service", "USER_SERVICE_PORT", 6104),
            (app, db) -> {
                UserController userController = new UserController(db);

                app.get("/users", AuthMiddleware.requireAdmin(userController::list));
                app.post("/users", AuthMiddleware.requireAdmin(userController::create));
                app.get("/users/{id}", AuthMiddleware.requireUser(userController::show));
                app.patch("/users/{id}", AuthMiddleware.requireUser(userController::update));
                app.delete("/users/{id}", AuthMiddleware.requireAdmin(userController::destroy));

                app.get("/admin/users", AuthMiddleware.requireAdmin(userController::list));
                app.get("/admin/users/{id}", AuthMiddleware.requireAdmin(userController::show));
                app.patch("/admin/users/{id}", AuthMiddleware.requireAdmin(userController::update));
                app.delete("/admin/users/{id}", AuthMiddleware.requireAdmin(userController::destroy));
            }
        );
    }
}
