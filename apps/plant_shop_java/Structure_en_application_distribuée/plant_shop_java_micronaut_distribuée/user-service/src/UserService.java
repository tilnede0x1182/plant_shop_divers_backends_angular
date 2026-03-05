import util.ServiceLauncher;

/**
 * Point d'entrée du service utilisateurs.
 */
public final class UserService {

    /**
 * Constructeur privé.
 */
private UserService() {}

    /**
 * Lance le service utilisateurs.
 * @param args Arguments de la ligne de commande
 */
public static void main(String[] args) {
        ServiceLauncher.run("user-service", "USER_SERVICE_PORT", 6104, args);
    }
}
