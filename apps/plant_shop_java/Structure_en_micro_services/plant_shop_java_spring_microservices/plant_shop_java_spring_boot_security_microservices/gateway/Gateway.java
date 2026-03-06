import gateway.core.Main;

/** Point d'entree de la Gateway API */
public final class Gateway {
    /** Lance la gateway */
    public static void main(String[] args) throws Exception {
        Main.create().start();
    }
}
