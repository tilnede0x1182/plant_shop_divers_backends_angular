// src/controllers/ApplicationController.java
package controller;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.ForbiddenResponse;
import io.javalin.security.RouteRole;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import model.User;
import repository.UserRepository;

import static io.javalin.apibuilder.ApiBuilder.*;

public final class ApplicationController {

    public enum Roles implements RouteRole {
        ANYONE, USER, ADMIN
    }

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
            path("/api", () -> {
                // Routes d'authentification
                path("/auth", () -> {
                    post("/register", authController::register, Roles.ANYONE);
                    post("/login", authController::login, Roles.ANYONE);
                    post("/logout", authController::logout, Roles.USER);
                    get("/me", authController::me, Roles.USER);
                });

                // Routes publiques pour les plantes
                path("/plants", () -> {
                    get(plantController::listPublic, Roles.ANYONE);
                    get("/{id}", plantController::show, Roles.ANYONE);
                });

                // Routes admin pour les plantes
                path("/admin/plants", () -> {
                    get(plantController::listAdmin, Roles.ADMIN);
                    post(plantController::create, Roles.ADMIN);
                    patch("/{id}", plantController::update, Roles.ADMIN);
                    delete("/{id}", plantController::destroy, Roles.ADMIN);
                });

                // Routes pour les utilisateurs
                path("/users", () -> {
                    get(userController::list, Roles.ADMIN);
                    post(userController::create, Roles.ADMIN);
                    get("/{id}", userController::show, Roles.USER);
                    patch("/{id}", userController::update, Roles.USER);
                    delete("/{id}", userController::destroy, Roles.ADMIN);
                });

                // Routes admin pour les utilisateurs
                path("/admin/users", () -> {
                    get(userController::list, Roles.ADMIN);
                    delete("/{id}", userController::destroy, Roles.ADMIN);
                });

                // Routes pour les commandes
                path("/orders", () -> {
                    get(orderController::list, Roles.USER);
                    post(orderController::create, Roles.USER);
                    patch("/{id}", orderController::patch, Roles.ADMIN);
                    delete("/{id}", orderController::destroy, Roles.ADMIN);
                });
            });
        };
    }

    /**
     * Gestionnaire d'accès pour toutes les routes sécurisées.
     */
    public void accessManager(Handler handler, Context ctx, Set<RouteRole> permittedRoles) throws Exception {
        if (permittedRoles.contains(Roles.ANYONE)) {
            handler.handle(ctx);
            return;
        }

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
            ctx.attribute("user", user);

            if (permittedRoles.contains(Roles.ADMIN) && !user.isAdmin) {
                throw new ForbiddenResponse("Accès refusé");
            }

            handler.handle(ctx);
        } catch (SQLException e) {
            throw new Exception("Erreur base de données lors de l'authentification", e);
        }
    }

		/* Enregistre le groupe de routes exposé par ce contrôleur sur l'instance Javalin */
		public void register(io.javalin.Javalin app) {
			app.routes(() -> getRoutes());
		}
}
