// src/controllers/ApplicationController.java
package controller;

import io.javalin.apibuilder.EndpointGroup;
import java.sql.Connection;
import java.util.Map;
import util.AuthMiddleware;

import static io.javalin.apibuilder.ApiBuilder.*;

public final class ApplicationController {

    private final OrderController orderController;

    public ApplicationController(Connection db, Map<String, String> env) {
        this.orderController = new OrderController(db, resolveCatalogUrl(env));
    }

    /**
     * Définit toutes les routes de l'API.
     */
    public EndpointGroup getRoutes() {
        return () -> path("/orders", () -> {
            get(AuthMiddleware.requireUser(orderController::list));
            post(AuthMiddleware.requireUser(orderController::create));
            patch("/{id}", AuthMiddleware.requireAdmin(orderController::patch));
            delete("/{id}", AuthMiddleware.requireAdmin(orderController::destroy));
        });
    }

    private static String resolveCatalogUrl(Map<String, String> env) {
        if (env.containsKey("CATALOG_SERVICE_URL")) {
            return env.get("CATALOG_SERVICE_URL");
        }
        String host = env.getOrDefault("SERVICE_HOST", "http://localhost");
        String port = env.getOrDefault("CATALOG_SERVICE_PORT", "6102");
        return host + ":" + port;
    }
}
