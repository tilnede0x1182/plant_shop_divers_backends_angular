import util.ServiceLauncher;

/**
 * Point d'entrée du microservice de commandes.
 */
public final class OrderService {

    /**
     * Constructeur privé.
     */
    private OrderService() {}

    /**
     * Lance le service de commandes.
     * @param args Arguments CLI
     */
    public static void main(String[] args) {
        ServiceLauncher.run("order-service", "ORDER_SERVICE_PORT", 6103, args);
    }
}
