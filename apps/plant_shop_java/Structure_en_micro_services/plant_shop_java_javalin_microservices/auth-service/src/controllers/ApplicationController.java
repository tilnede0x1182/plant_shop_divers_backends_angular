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

/**
 * Contrôleur principal de l'application d'authentification.
 * Gère les routes et les handlers HTTP.
 */
public final class ApplicationController {

    private final AuthController authController;
    private final UserRepository userRepoForAuth;

    /**
     * Constructeur avec connexion à la base de données.
     * @param db Connexion à la base de données
     */
    public ApplicationController(Connection db) {
        this.authController = new AuthController(db);
        this.userRepoForAuth = new UserRepository(db);
    }

    /**
     * Définit toutes les routes de l'API.
     */
    public EndpointGroup getRoutes() {
        return () -> {
            // Routes d'authentification
            path("/auth", () -> {
                post("/register", authController::register);
                post("/login", authController::login);
                post("/logout", requireUser(authController::logout));
                get("/me", requireUser(authController::me));
                get("/_session", authController::sessionStatus);
            });
        };
    }

    /**
     * Wrapper pour exiger une authentification utilisateur.
     * @param handler Handler à protéger
     * @return Handler protégé
     */
    private Handler requireUser(Handler handler) {
        return ctx -> handleWithUser(ctx, handler, false);
    }

    /**
     * Wrapper pour exiger une authentification admin.
     * @param handler Handler à protéger
     * @return Handler protégé
     */
    private Handler requireAdmin(Handler handler) {
        return ctx -> handleWithUser(ctx, handler, true);
    }

    /**
     * Gère une requête avec authentification.
     * @param ctx Contexte de la requête
     * @param handler Handler à exécuter
     * @param adminOnly Si true, exige un admin
     */
    private void handleWithUser(Context ctx, Handler handler, boolean adminOnly) throws Exception {
        User user = authenticate(ctx);
        if (adminOnly && !user.isAdmin) {
            throw new ForbiddenResponse("Accès refusé");
        }
        ctx.attribute("user", user);
        handler.handle(ctx);
    }

    /**
     * Authentifie l'utilisateur via le cookie de session.
     * @param ctx Contexte de la requête
     * @return Utilisateur authentifié
     */
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
