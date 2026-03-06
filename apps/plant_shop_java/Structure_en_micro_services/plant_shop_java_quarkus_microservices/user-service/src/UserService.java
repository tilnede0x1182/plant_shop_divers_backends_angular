import util.ServiceLauncher;

/**
 * Point d'entree du service utilisateurs.
 * Lance le serveur Quarkus sur le port configure.
 */
public final class UserService {

    /**
     * Constructeur prive pour empecher l'instanciation.
     */
    private UserService() {}

    /**
     * Point d'entree principal du service.
     *
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.run("user-service", "USER_SERVICE_PORT", 6104, args);
    }
}
