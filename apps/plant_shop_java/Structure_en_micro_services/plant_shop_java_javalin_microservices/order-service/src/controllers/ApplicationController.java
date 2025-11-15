// src/controllers/ApplicationController.java
package controller;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.sql.Connection;
import model.UserDTO;

import static io.javalin.apibuilder.ApiBuilder.*;

public final class ApplicationController {

    private final OrderController orderController;

    public ApplicationController(Connection db) {
        this.orderController = new OrderController(db);
    }

    /**
     * Définit toutes les routes de l'API.
     */
    public EndpointGroup getRoutes() {
        return () -> {
            path("/api", () -> {
                // Routes pour les commandes uniquement
                path("/orders", () -> {
                    get(requireUser(orderController::list));
                    post(requireUser(orderController::create));
                    patch("/{id}", requireAdmin(orderController::patch));
                    delete("/{id}", requireAdmin(orderController::destroy));
                });
            });
        };
    }

    private Handler requireUser(Handler handler) {
        return ctx -> handleWithUser(ctx, handler, false);
    }

    private Handler requireAdmin(Handler handler) {
        return ctx -> handleWithUser(ctx, handler, true);
    }

    private void handleWithUser(Context ctx, Handler handler, boolean adminOnly) throws Exception {
        // Lire les headers propagés par la gateway
        String userIdHeader = ctx.header("X-User-Id");
        String adminHeader = ctx.header("X-User-Admin");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new UnauthorizedResponse("Non authentifié");
        }

        try {
            int userId = Integer.parseInt(userIdHeader);
            boolean isAdmin = "true".equalsIgnoreCase(adminHeader);

            if (adminOnly && !isAdmin) {
                throw new ForbiddenResponse("Accès administrateur requis");
            }

            // Créer un User DTO minimal et le mettre dans le contexte
            UserDTO user = new UserDTO(userId, isAdmin);
            ctx.attribute("user", user);
            handler.handle(ctx);
        } catch (NumberFormatException e) {
            throw new UnauthorizedResponse("ID utilisateur invalide");
        }
    }
}
