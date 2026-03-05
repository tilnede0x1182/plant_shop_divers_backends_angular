/**
 * Point d'entrée de la gateway API.
 */
public final class Gateway {
    /**
 * Lance la gateway.
 * @param args Arguments de la ligne de commande
 */
public static void main(String[] args) throws Exception {
        GatewayRuntime.create().start();
    }
}
