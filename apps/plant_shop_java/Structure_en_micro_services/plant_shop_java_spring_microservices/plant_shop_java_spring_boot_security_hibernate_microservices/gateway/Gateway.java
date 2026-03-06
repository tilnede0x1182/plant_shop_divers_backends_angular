import gateway.core.Main;

/** Point d'entrée de la Gateway. */
public final class Gateway {
    /** Lance le serveur Gateway. */
    public static void main(String[] args) throws Exception {
        Main.create().start();
    }
}
