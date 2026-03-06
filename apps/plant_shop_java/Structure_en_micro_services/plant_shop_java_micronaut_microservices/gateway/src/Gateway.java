import gateway.core.Main;

/**
 * Point d'entrée de la gateway.
 */
public final class Gateway {
    /**
     * Lance la gateway.
     * @param args Arguments CLI (non utilisés)
     * @throws Exception En cas d'erreur de démarrage
     */
    public static void main(String[] args) throws Exception {
        Main.create().start();
    }
}
