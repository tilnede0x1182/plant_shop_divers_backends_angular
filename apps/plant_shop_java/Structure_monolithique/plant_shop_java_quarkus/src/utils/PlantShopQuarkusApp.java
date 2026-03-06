package utils;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;

/**
 * Application principale Quarkus.
 */
public final class PlantShopQuarkusApp implements QuarkusApplication {

    /**
     * Callback au démarrage de l'application.
     * @param event StartupEvent Événement de démarrage
     */
    public void onStart(@Observes StartupEvent event) {
        String port = System.getProperty("quarkus.http.port", "4100");
        System.out.printf("🚀 Serveur Quarkus démarré sur http://localhost:%s%n", port);
    }

    /**
     * Point d'entrée de l'application Quarkus.
     * @param args String[] Arguments de la ligne de commande
     * @return int Code de sortie
     * @throws Exception En cas d'erreur
     */
    @Override
    public int run(String... args) throws Exception {
        Quarkus.waitForExit();
        return 0;
    }
}
