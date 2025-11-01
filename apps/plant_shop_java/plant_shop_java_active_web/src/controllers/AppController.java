package controllers;

import models.User;
import org.javalite.activejdbc.Base;
import org.javalite.common.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.DatabaseFactory;
import util.SessionManager;

import java.util.Map;
import java.util.Objects;

public abstract class AppController extends org.javalite.activeweb.AppController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppController.class);

    private User currentUser;

    @FunctionalInterface
    protected interface ControllerAction {
        void execute() throws Exception;
    }

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

    protected User getCurrentUser() {
        return currentUser;
    }

    protected void requireLogin() {
        if (currentUser == null) {
            throw new SecurityException("Authentification requise.");
        }
    }

    protected void requireAdmin() {
        requireLogin();
        if (!Objects.equals(Boolean.TRUE, currentUser.getBoolean("is_admin"))) {
            throw new SecurityException("Accès administrateur requis.");
        }
    }

    protected void respondJson(int status, String json) {
        getHttpServletResponse().setStatus(status);
        getHttpServletResponse().setContentType("application/json");
        respond(json);
    }

    protected void respondEmpty(int status) {
        getHttpServletResponse().setStatus(status);
        respond("");
    }

    private void respondError(int status, String message) {
        Map<String, String> payload = Map.of("error", message == null ? "Erreur" : message);
        respondJson(status, JsonHelper.toJsonString(payload));
    }

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

    private void closeConnection() {
        if (Base.hasConnection()) {
            Base.close();
        }
    }

    private void resolveCurrentUser() {
        String sessionId = cookieValue("session_id");
        if (sessionId == null) {
            return;
        }
        Long userId = SessionManager.getUserId(sessionId);
        if (userId == null) {
            return;
        }
        currentUser = User.findById(userId);
    }
}
