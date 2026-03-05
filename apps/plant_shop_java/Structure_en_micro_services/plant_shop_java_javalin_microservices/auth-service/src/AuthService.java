import controller.ApplicationController;
import util.ServiceRuntime;

/**
 * Service d'authentification Javalin.
 */
public final class AuthService {

    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     * @throws Exception En cas d'erreur au démarrage
     */
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
