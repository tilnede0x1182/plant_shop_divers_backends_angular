import util.ServiceLauncher;

/**
 * Point d'entrée du service d'authentification.
 * Lance le serveur Quarkus sur le port configuré.
 */
public final class AuthService {

    /**
     * Constructeur privé pour empêcher l'instanciation.
     */
    private AuthService() {}

    /**
     * Point d'entrée principal du service.
     *
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.run("auth-service", "AUTH_SERVICE_PORT", 6101, args);
    }
}
