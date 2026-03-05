/**
 * Point d'entrée de la passerelle API.
 */
public final class Gateway {
    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     * @throws Exception En cas d'erreur au démarrage
     */
    public static void main(String[] args) throws Exception {
        GatewayRuntime.create().start();
    }
}
