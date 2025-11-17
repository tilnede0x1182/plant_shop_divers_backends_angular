package utils;

import controllers.AuthController;
import controllers.OrderController;
import controllers.PlantController;
import controllers.UserController;
import jakarta.ws.rs.core.Application;
import security.CorsConfig;
import security.SessionAuthFilter;
import java.util.Set;

/**
 * Configuration JAX-RS Application.
 * Utilise getClasses() au lieu de getSingletons() (dépréciée en Jakarta REST 3.1+).
 * Les instances sont gérées automatiquement par CDI via RESTEasy.
 */
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
            AuthController.class,
            PlantController.class,
            UserController.class,
            OrderController.class,
            SessionAuthFilter.class,
            CorsConfig.class
        );
    }
}
