// src/controllers/ApplicationController.java
package controller;

import io.javalin.apibuilder.EndpointGroup;
import java.sql.Connection;
import util.AuthMiddleware;

import static io.javalin.apibuilder.ApiBuilder.*;

/**
 * Contrôleur principal orchestrant les routes du user-service.
 */
public final class ApplicationController {

    private final UserController userController;

    /**
     * Initialise le contrôleur avec la connexion BDD.
     * @param db Connexion à la base de données
     */
    public ApplicationController(Connection db) {
        this.userController = new UserController(db);
    }

    /**
     * Définit toutes les routes de l'API.
     */
    public EndpointGroup getRoutes() {
        return () -> {
            path("/users", () -> {
                get(AuthMiddleware.requireAdmin(userController::list));
                post(AuthMiddleware.requireAdmin(userController::create));
                get("/{id}", AuthMiddleware.requireUser(userController::show));
                patch("/{id}", AuthMiddleware.requireUser(userController::update));
                delete("/{id}", AuthMiddleware.requireAdmin(userController::destroy));
            });

            path("/admin/users", () -> {
                get(AuthMiddleware.requireAdmin(userController::list));
                get("/{id}", AuthMiddleware.requireAdmin(userController::show));
                patch("/{id}", AuthMiddleware.requireAdmin(userController::update));
                delete("/{id}", AuthMiddleware.requireAdmin(userController::destroy));
            });
        };
    }
}
