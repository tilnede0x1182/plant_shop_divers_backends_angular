// src/controllers/ApplicationController.java
package controller;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import model.User;
import repository.UserRepository;

import static io.javalin.apibuilder.ApiBuilder.*;

public final class ApplicationController {

    private final AuthController authController;
    private final PlantController plantController;
    private final UserController userController;
    private final OrderController orderController;
    private final UserRepository userRepoForAuth;

    public ApplicationController(Connection db) {
        this.authController = new AuthController(db);
        this.plantController = new PlantController(db);
        this.userController = new UserController(db);
        this.orderController = new OrderController(db);
        this.userRepoForAuth = new UserRepository(db);
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
                // Routes d'authentification
                path("/auth", () -> {
                    post("/register", authController::register);
                    post("/login", authController::login);
                    post("/logout", requireUser(authController::logout));
                    get("/me", requireUser(authController::me));
                });

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

                // Routes pour les commandes
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
        User user = authenticate(ctx);
        if (adminOnly && !user.isAdmin) {
            throw new ForbiddenResponse("Accès refusé");
        }
        ctx.attribute("user", user);
        handler.handle(ctx);
    }

    private User authenticate(Context ctx) throws Exception {
        String sessionId = ctx.cookie("session_id");
        if (sessionId == null) {
            throw new UnauthorizedResponse("Non authentifié");
        }

        Map<String, Integer> sessions = AuthController.getSessions();
        Integer userId = sessions.get(sessionId);
        if (userId == null) {
            throw new UnauthorizedResponse("Session invalide");
        }

        try {
            User user = userRepoForAuth.find(userId);
            if (user == null) {
                throw new UnauthorizedResponse("Utilisateur introuvable");
            }
            return user;
        } catch (SQLException e) {
            throw new Exception("Erreur base de données lors de l'authentification", e);
        }
    }
}
