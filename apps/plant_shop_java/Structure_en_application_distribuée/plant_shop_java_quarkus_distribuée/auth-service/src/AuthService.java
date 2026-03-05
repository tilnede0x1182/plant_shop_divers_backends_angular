import util.ServiceLauncher;

/**
 * Point d'entrée du service d'authentification.
 */
public final class AuthService {

    /**
 * Constructeur privé.
 */
private AuthService() {}

    /**
 * Lance le service d'authentification.
 * @param args Arguments de la ligne de commande
 */
public static void main(String[] args) {
        ServiceLauncher.run("auth-service", "AUTH_SERVICE_PORT", 6101, args);
    }
}
