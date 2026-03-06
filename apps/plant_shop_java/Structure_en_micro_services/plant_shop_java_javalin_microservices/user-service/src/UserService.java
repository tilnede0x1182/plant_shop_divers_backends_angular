import controller.ApplicationController;
import util.ServiceRuntime;

/**
 * Point d'entrée du microservice de gestion des utilisateurs.
 */
public final class UserService {

    /**
     * Lance le service utilisateur sur le port configuré.
     * @param args Arguments CLI (non utilisés)
     * @throws Exception En cas d'erreur de démarrage
     */
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
