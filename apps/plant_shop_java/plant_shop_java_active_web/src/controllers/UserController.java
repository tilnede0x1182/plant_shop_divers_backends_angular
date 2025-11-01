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

public final class UserController extends AppController {

    @GET
    public void index() {
        runAction(() -> {
            requireAdmin();
            LazyList<User> users = User.findAll().orderBy("is_admin desc, name asc");
            respondJson(200, users.toJson(false, "id", "name", "email", "is_admin"));
        });
    }

    @GET
    public void show() {
        runAction(() -> {
            User current = getCurrentUser();
            Integer userId = parseId(getId());
            if (current == null || (!current.getBoolean("is_admin") && !current.getId().equals(id))) {
                throw new SecurityException("Accès non autorisé.");
            }
            if (user == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Utilisateur introuvable")));
                return;
            }
            respondJson(200, user.toJson(false, "id", "name", "email", "is_admin"));
        });
    }

    @POST
    public void create() {
        runAction(() -> {
            requireAdmin();
            Map<String, Object> body = ApiMapper.jsonToMap(getRequestString());
            String name = (String) body.get("name");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            Boolean isAdmin = (Boolean) body.getOrDefault("is_admin", Boolean.FALSE);

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

            respondJson(201, user.toJson(false, "id", "name", "email", "is_admin"));
        });
    }

    @PATCH
    public void update(int id) {
        runAction(() -> {
            User current = getCurrentUser();
            if (current == null) {
                respondEmpty(401);
                return;
            }
            User user = User.findById(id);
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

            if (body.containsKey("is_admin") && Boolean.TRUE.equals(current.getBoolean("is_admin"))) {
                user.set("is_admin", Boolean.TRUE.equals(body.get("is_admin")));
            }

            if (!user.saveIt()) {
                respondJson(400, JsonHelper.toJsonString(user.errors()));
                return;
            }

            respondJson(200, user.toJson(false, "id", "name", "email", "is_admin"));
        });
    }

    @DELETE
    public void destroy(int id) {
        runAction(() -> {
            requireAdmin();
            User user = User.findById(id);
            if (user == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Utilisateur introuvable")));
                return;
            }
            user.delete();
            respondEmpty(200);
        });
    }
}
