/**
 * Point d'entree de la Gateway HTTP.
 * Cree et demarre le serveur de routage.
 */
public final class Gateway {
    /**
     * Point d'entree principal de la Gateway.
     *
     * @param args Arguments de ligne de commande
     * @throws Exception En cas d'erreur au demarrage
     */
    public static void main(String[] args) throws Exception {
        GatewayRuntime.create().start();
    }
}
