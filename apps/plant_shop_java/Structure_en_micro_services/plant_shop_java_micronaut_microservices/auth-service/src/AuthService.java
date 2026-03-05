import util.ServiceLauncher;

/**
 * Point d'entrée du service d'authentification.
 */
public final class AuthService {

    /**
     * Constructeur privé (classe utilitaire).
     */
    private AuthService() {}

    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.run("auth-service", "AUTH_SERVICE_PORT", 6101, args);
    }
}
