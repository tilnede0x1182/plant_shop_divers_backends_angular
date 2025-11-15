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

    private final UserController userController;

    public ApplicationController(Connection db) {
        this.userController = new UserController(db);
    }

    /**
     * Définit toutes les routes de l'API.
     */
    public EndpointGroup getRoutes() {
        return () -> {
            path("/api", () -> {
                // Routes pour les utilisateurs
                path("/users", () -> {
                    get(requireAdmin(userController::list));
                    post(requireAdmin(userController::create));
                    get("/{id}", requireUser(userController::show));
                    patch("/{id}", requireUser(userController::update));
                    delete("/{id}", requireAdmin(userController::destroy));
                });

                // Routes admin pour les utilisateurs
                path("/admin/users", () -> {
                    get(requireAdmin(userController::list));
                    get("/{id}", requireAdmin(userController::show));
                    patch("/{id}", requireAdmin(userController::update));
                    delete("/{id}", requireAdmin(userController::destroy));
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
