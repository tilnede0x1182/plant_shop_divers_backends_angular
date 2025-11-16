// src/controllers/ApplicationController.java
package controller;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Handler;
import java.sql.Connection;
import util.AuthMiddleware;

import static io.javalin.apibuilder.ApiBuilder.*;

public final class ApplicationController {

    private final PlantController plantController;

    public ApplicationController(Connection db) {
        this.plantController = new PlantController(db);
    }

    /**
     * Définit toutes les routes de l'API.
     */
    public EndpointGroup getRoutes() {
        return () -> {
            path("/internal/plants", () -> {
                patch("/{id}/stock", plantController::updateStock);
            });

            path("/plants", () -> {
                get(plantController::listPublic);
                get("/{id}", plantController::show);
            });

            path("/admin/plants", () -> {
                get(requireAdmin(plantController::listAdmin));
                post(requireAdmin(plantController::create));
                patch("/{id}", requireAdmin(plantController::update));
                delete("/{id}", requireAdmin(plantController::destroy));
            });
        };
    }

    private Handler requireAdmin(Handler handler) {
        return AuthMiddleware.requireAdmin(handler);
    }
}
