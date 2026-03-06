import util.ServiceLauncher;

/**
 * Point d'entree du service de commandes.
 * Lance le serveur Quarkus sur le port configure.
 */
public final class OrderService {

    /**
     * Constructeur prive pour empecher l'instanciation.
     */
    private OrderService() {}

    /**
     * Point d'entree principal du service.
     *
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.run("order-service", "ORDER_SERVICE_PORT", 6103, args);
    }
}
