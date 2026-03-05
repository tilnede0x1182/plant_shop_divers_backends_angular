package app.controllers;

import models.User;
import org.javalite.activejdbc.LazyList;
import org.javalite.activeweb.annotations.DELETE;
import org.javalite.activeweb.annotations.GET;
import org.javalite.activeweb.annotations.PATCH;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;
import util.ApiMapper;
import util.PasswordUtil;

import java.util.Map;

/**
 * Contrôleur pour les utilisateurs.
 * Gère les opérations CRUD sur les utilisateurs.
 */
public final class UserController extends AppController {

    /**
     * Liste tous les utilisateurs (admin).
     */
    @GET
    public void index() {
        runAction(() -> {
            requireAdmin();
            LazyList<User> users = User.findAll().orderBy("is_admin desc, name asc");
            respondJson(200, usersJson(users));
        });
    }

    /**
     * Affiche un utilisateur.
     */
    @GET
    public void show() {
        runAction(() -> {
            User current = getCurrentUser();
            Integer userId = parseId(getId());
            if (current == null || userId == null) {
                respondEmpty(401);
                return;
            }
            boolean isAdmin = Boolean.TRUE.equals(current.getBoolean("is_admin"));
            if (!isAdmin && !current.getLongId().equals(userId.longValue())) {
                throw new SecurityException("Accès non autorisé.");
            }
            User user = User.findById(userId);
            if (user == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Utilisateur introuvable")));
                return;
            }
            respondJson(200, userJson(user));
        });
    }

    /**
     * Crée un utilisateur.
     */
    @POST
    public void create() {
        runAction(() -> {
            requireAdmin();
            Map<String, Object> body = ApiMapper.jsonToMap(getRequestString());
            String name = (String) body.get("name");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            Boolean isAdmin = extractBoolean(body, "is_admin");
            if (isAdmin == null) {
                isAdmin = extractBoolean(body, "admin");
            }
            if (isAdmin == null) {
                isAdmin = Boolean.FALSE;
            }

            if (name == null || email == null || password == null) {
                respondJson(400, JsonHelper.toJsonString(Map.of("error", "name, email et password requis")));
                return;
            }

            User user = new User();
            user.set("name", name);
            user.set("email", email.toLowerCase());
            user.set("password_hash", PasswordUtil.hashPassword(password));
            user.set("is_admin", Boolean.TRUE.equals(isAdmin));

            if (!user.saveIt()) {
                respondJson(400, JsonHelper.toJsonString(user.errors()));
                return;
            }

            respondJson(201, userJson(user));
        });
    }

    /**
     * Met à jour un utilisateur.
     */
    @PATCH
    public void update() {
        runAction(() -> {
            User current = getCurrentUser();
            if (current == null) {
                respondEmpty(401);
                return;
            }
            Integer userId = parseId(getId());
            User user = (userId == null) ? null : User.findById(userId);
            if (user == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Utilisateur introuvable")));
                return;
            }

            Map<String, Object> body = ApiMapper.jsonToMap(getRequestString());

            if (body.containsKey("name")) {
                user.set("name", body.get("name"));
            }
            if (body.containsKey("email")) {
                user.set("email", ((String) body.get("email")).toLowerCase());
            }
            if (body.containsKey("password")) {
                user.set("password_hash", PasswordUtil.hashPassword((String) body.get("password")));
            }

            Boolean adminFlag = null;
            if (body.containsKey("is_admin")) {
                adminFlag = extractBoolean(body, "is_admin");
            } else if (body.containsKey("admin")) {
                adminFlag = extractBoolean(body, "admin");
            }
            if (adminFlag != null && Boolean.TRUE.equals(current.getBoolean("is_admin"))) {
                user.set("is_admin", adminFlag);
            }

            if (!user.isModified()) {
                respondJson(200, userJson(user));
                return;
            }

            if (!user.saveIt()) {
                respondJson(400, JsonHelper.toJsonString(user.errors()));
                return;
            }

            respondJson(200, userJson(user));
        });
    }

    /**
     * Supprime un utilisateur.
     */
    @DELETE
    public void destroy() {
        runAction(() -> {
            requireAdmin();
            Integer userId = parseId(getId());
            User user = (userId == null) ? null : User.findById(userId);
            if (user == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Utilisateur introuvable")));
                return;
            }
            user.delete();
            respondEmpty(200);
        });
    }

    /**
     * Extrait un booléen d une Map.
     * @param body Map Corps de requête
     * @param key String Clé à extraire
     * @return Boolean Valeur ou null
     */
    private Boolean extractBoolean(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof String s) {
            if ("true".equalsIgnoreCase(s)) return Boolean.TRUE;
            if ("false".equalsIgnoreCase(s)) return Boolean.FALSE;
        }
        return null;
    }

    /**
     * Parse un ID depuis une chaîne.
     * @param value String Valeur à parser
     * @return Integer ID ou null
     */
    private Integer parseId(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
