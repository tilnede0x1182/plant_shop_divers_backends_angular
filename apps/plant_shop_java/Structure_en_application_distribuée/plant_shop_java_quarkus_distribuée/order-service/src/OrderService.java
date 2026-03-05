import util.ServiceLauncher;

/**
 * Point d'entrée du service commandes.
 */
public final class OrderService {

    /**
     * Constructeur privé pour empêcher l'instanciation.
     */
    private OrderService() {}

    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.run("order-service", "ORDER_SERVICE_PORT", 6103, args);
    }
}
