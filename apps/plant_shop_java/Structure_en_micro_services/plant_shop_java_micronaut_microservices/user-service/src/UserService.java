import util.ServiceLauncher;

/**
 * Point d'entrée du microservice utilisateur.
 */
public final class UserService {

    /**
     * Constructeur privé.
     */
    private UserService() {}

    /**
     * Lance le service utilisateur.
     * @param args Arguments CLI
     */
    public static void main(String[] args) {
        ServiceLauncher.run("user-service", "USER_SERVICE_PORT", 6104, args);
    }
}
