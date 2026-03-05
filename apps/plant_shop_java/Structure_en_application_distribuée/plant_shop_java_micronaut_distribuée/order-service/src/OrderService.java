import util.ServiceLauncher;

/**
 * Point d'entrée du service commandes.
 */
public final class OrderService {

    /**
 * Constructeur privé.
 */
private OrderService() {}

    /**
 * Lance le service commandes.
 * @param args Arguments de la ligne de commande
 */
public static void main(String[] args) {
        ServiceLauncher.run("order-service", "ORDER_SERVICE_PORT", 6103, args);
    }
}
