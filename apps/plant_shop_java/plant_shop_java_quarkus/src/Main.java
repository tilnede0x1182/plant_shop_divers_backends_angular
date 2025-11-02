import utils.QuarkusBootstrap;

/**
 * Point d'entrée principal de l'application.
 * Le Makefile compile ce fichier (Main.java) mais exécute la classe "Application".
 * Ce fichier contient donc la classe "Application".
 */
public class Main {

    public static void main(String[] args) {
        try {
            // Délègue le démarrage à notre classe de bootstrap
            QuarkusBootstrap.run(args);
        } catch (Exception e) {
            System.err.println("❌ Erreur au démarrage : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
