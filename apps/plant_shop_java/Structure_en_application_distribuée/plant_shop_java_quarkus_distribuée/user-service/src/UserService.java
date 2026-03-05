import util.ServiceLauncher;

/**
 * Point d'entrée du service utilisateurs.
 */
public final class UserService {

    /**
     * Constructeur privé pour empêcher l'instanciation.
     */
    private UserService() {}

    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.run("user-service", "USER_SERVICE_PORT", 6104, args);
    }
}
