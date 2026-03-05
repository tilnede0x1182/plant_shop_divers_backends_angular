package app.controllers;

import models.User;
import org.javalite.activejdbc.Base;
import org.javalite.activeweb.Cookie;
import org.javalite.common.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.DatabaseFactory;
import util.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Controleur de base pour tous les controleurs de l application.
 * Fournit des utilitaires pour la gestion des utilisateurs, erreurs et reponses JSON.
 */
public abstract class AppController extends org.javalite.activeweb.AppController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppController.class);

    private User currentUser;

    /**
     * Interface fonctionnelle pour encapsuler une action de controleur.
     */
    @FunctionalInterface
    protected interface ControllerAction {
        /**
         * Execute l action du controleur.
         * @throws Exception Exception en cas d erreur
         */
        void execute() throws Exception;
    }

    /**
     * Execute une action en gerant connexion DB et utilisateur courant.
     * @param action ControllerAction Action a executer
     */
    protected void runAction(ControllerAction action) {
        try {
            openConnectionIfNeeded();
            resolveCurrentUser();
            action.execute();
        } catch (SecurityException se) {
            respondError(403, se.getMessage());
        } catch (IllegalArgumentException iae) {
            respondError(400, iae.getMessage());
        } catch (Exception e) {
            LOGGER.error("Erreur ActiveWeb", e);
            e.printStackTrace(System.err);
            respondError(500, "Erreur interne du serveur.");
        } finally {
            currentUser = null;
            closeConnection();
        }
    }

    /**
     * Retourne l utilisateur actuellement authentifie.
     * @return User Utilisateur courant ou null
     */
    protected User getCurrentUser() {
        return currentUser;
    }

    /**
     * Verifie que l utilisateur est authentifie.
     * @throws SecurityException Si utilisateur non authentifie
     */
    protected void requireLogin() {
        if (currentUser == null) {
            throw new SecurityException("Authentification requise.");
        }
    }

    /**
     * Verifie que l utilisateur est authentifie et administrateur.
     * @throws SecurityException Si utilisateur non authentifie ou non admin
     */
    protected void requireAdmin() {
        requireLogin();
        if (!Objects.equals(Boolean.TRUE, currentUser.getBoolean("is_admin"))) {
            throw new SecurityException("Accès administrateur requis.");
        }
    }

    /**
     * Envoie une reponse JSON.
     * @param status int Code HTTP
     * @param json String Corps JSON
     */
    protected void respondJson(int status, String json) {
        respond(json)
            .contentType("application/json")
            .status(status);
    }

    /**
     * Envoie une reponse vide.
     * @param status int Code HTTP
     */
    protected void respondEmpty(int status) {
        respond("")
            .status(status);
    }

    /**
     * Convertit un utilisateur en JSON.
     * @param user User Utilisateur a convertir
     * @return String JSON de l utilisateur
     */
    protected String userJson(User user) {
        if (user == null) {
            return "{}";
        }
        return JsonHelper.toJsonString(userPayload(user));
    }

    /**
     * Convertit une collection d utilisateurs en JSON.
     * @param users Iterable<User> Collection d utilisateurs
     * @return String JSON array des utilisateurs
     */
    protected String usersJson(Iterable<User> users) {
        if (users == null) {
            return "[]";
        }
        List<Map<String, Object>> payload = new ArrayList<>();
        for (User user : users) {
            payload.add(userPayload(user));
        }
        return JsonHelper.toJsonString(Collections.unmodifiableList(payload));
    }

    /**
     * Envoie une reponse d erreur JSON.
     * @param status int Code HTTP
     * @param message String Message d erreur
     */
    private void respondError(int status, String message) {
        Map<String, String> payload = Map.of("error", message == null ? "Erreur" : message);
        respondJson(status, JsonHelper.toJsonString(payload));
    }

    /**
     * Ouvre une connexion DB si necessaire.
     */
    private void openConnectionIfNeeded() {
        if (!Base.hasConnection()) {
            Base.open(
                "org.postgresql.Driver",
                DatabaseFactory.jdbcUrlOrDefault(),
                DatabaseFactory.dbUserOrDefault(),
                DatabaseFactory.dbPassOrDefault()
            );
        }
    }

    /**
     * Ferme la connexion DB si ouverte.
     */
    private void closeConnection() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    /**
     * Resout l utilisateur courant depuis le cookie de session.
     */
    private void resolveCurrentUser() {
        Cookie sessionCookie = cookie("session_id");
        if (sessionCookie == null) {
            return;
        }
        String sessionId = sessionCookie.getValue();
        Long userId = SessionManager.getUserId(sessionId);
        if (userId == null) {
            return;
        }
        currentUser = User.findById(userId);
    }

    /**
     * Cree un payload JSON pour un utilisateur.
     * @param user User Utilisateur
     * @return Map<String,Object> Payload JSON
     */
    private Map<String, Object> userPayload(User user) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", user.get("id"));
        payload.put("name", user.get("name"));
        payload.put("email", user.get("email"));
        Boolean isAdmin = user.getBoolean("is_admin");
        payload.put("is_admin", isAdmin);
        payload.put("admin", Boolean.TRUE.equals(isAdmin));
        return payload;
    }
}
