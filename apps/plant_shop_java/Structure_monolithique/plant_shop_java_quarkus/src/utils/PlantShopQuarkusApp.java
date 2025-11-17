package utils;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

/**
 * Application Quarkus minimale : elle attend simplement l'arrêt du runtime.
 */
public final class PlantShopQuarkusApp implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        Quarkus.waitForExit();
        return 0;
    }
}
