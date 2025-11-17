package utils;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;

public final class PlantShopQuarkusApp implements QuarkusApplication {

    public void onStart(@Observes StartupEvent event) {
        String port = System.getProperty("quarkus.http.port", "4100");
        System.out.printf("🚀 Serveur Quarkus démarré sur http://localhost:%s%n", port);
    }

    @Override
    public int run(String... args) throws Exception {
        Quarkus.waitForExit();
        return 0;
    }
}
