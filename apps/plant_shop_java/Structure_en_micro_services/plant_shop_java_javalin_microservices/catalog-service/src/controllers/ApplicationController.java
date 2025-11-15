// src/controllers/ApplicationController.java
package controller;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.sql.Connection;

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
            // Routes internes pour communication inter-services
            path("/internal/plants", () -> {
                patch("/{id}/stock", plantController::updateStock);
            });

            path("/api", () -> {
                // Routes publiques pour les plantes
                path("/plants", () -> {
                    get(plantController::listPublic);
                    get("/{id}", plantController::show);
                });

                // Routes admin pour les plantes
                path("/admin/plants", () -> {
                    get(requireAdmin(plantController::listAdmin));
                    post(requireAdmin(plantController::create));
                    patch("/{id}", requireAdmin(plantController::update));
                    delete("/{id}", requireAdmin(plantController::destroy));
                });
            });
        };
    }

    private Handler requireAdmin(Handler handler) {
        return ctx -> {
            // Lire les headers propagés par la gateway
            String userIdHeader = ctx.header("X-User-Id");
            String adminHeader = ctx.header("X-User-Admin");

            if (userIdHeader == null || userIdHeader.isBlank()) {
                throw new UnauthorizedResponse("Non authentifié");
            }

            boolean isAdmin = "true".equalsIgnoreCase(adminHeader);
            if (!isAdmin) {
                throw new ForbiddenResponse("Accès administrateur requis");
            }

            handler.handle(ctx);
        };
    }
}
