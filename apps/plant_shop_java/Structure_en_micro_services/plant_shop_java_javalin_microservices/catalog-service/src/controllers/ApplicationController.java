// src/controllers/ApplicationController.java
package controller;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Handler;
import java.sql.Connection;
import util.AuthMiddleware;

import static io.javalin.apibuilder.ApiBuilder.*;

/**
 * Contrôleur principal du service de catalogue.
 * Définit les routes pour les plantes.
 */
public final class ApplicationController {

    private final PlantController plantController;

    /**
     * Constructeur avec connexion à la base de données.
     * @param db Connexion à la base de données
     */
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

    /**
     * Wrapper pour exiger une authentification admin.
     * @param handler Handler à protéger
     * @return Handler protégé
     */
    private Handler requireAdmin(Handler handler) {
        return AuthMiddleware.requireAdmin(handler);
    }
}
