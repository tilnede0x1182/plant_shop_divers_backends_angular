import gateway.core.Main;

/**
 * Point d'entrée de la gateway.
 */
public final class Gateway {
    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) throws Exception {
        Main.create().start();
    }
}
