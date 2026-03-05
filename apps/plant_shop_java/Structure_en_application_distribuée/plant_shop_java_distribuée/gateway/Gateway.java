/**
 * Point d'entrée de la gateway.
 */
public final class Gateway {
    /**
     * Point d'entrée principal.
     * @param args String[] Arguments de ligne de commande
     */
    public static void main(String[] args) throws Exception {
        GatewayRuntime.create().start();
    }
}
