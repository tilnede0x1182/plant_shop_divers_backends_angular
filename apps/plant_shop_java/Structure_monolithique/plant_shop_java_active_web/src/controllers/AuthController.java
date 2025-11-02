package app.controllers;

import models.User;
import org.javalite.activeweb.Cookie;
import org.javalite.activeweb.annotations.GET;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;
import util.PasswordUtil;
import util.SessionManager;
import util.ApiMapper;

import java.util.Map;

public final class AuthController extends AppController {

    @POST
    public void login() {
        runAction(() -> {
            Map<String, Object> body = ApiMapper.jsonToMap(getRequestString());
            String email = (String) body.get("email");
            String password = (String) body.get("password");

            if (email == null || password == null) {
                respondJson(400, JsonHelper.toJsonString(Map.of("error", "email et password requis")));
                return;
            }

            User user = User.findFirst("email = ?", email);
            if (user == null || !PasswordUtil.checkPassword(password, user.getString("password_hash"))) {
                respondJson(401, JsonHelper.toJsonString(Map.of("error", "Identifiants invalides")));
                return;
            }

            Object userIdObj = user.getId();
            if (!(userIdObj instanceof Number)) {
                throw new IllegalStateException("Identifiant utilisateur non numérique");
            }
            String sessionId = SessionManager.createSession((Number) userIdObj);
            Cookie cookie = new Cookie("session_id", sessionId);
            cookie.setPath("/");
            cookie.setHttpOnly();
            cookie.setMaxAge(3600);
            sendCookie(cookie);

            respondJson(201, userJson(user));
        });
    }

    @POST
    public void register() {
        runAction(() -> {
            Map<String, Object> body = ApiMapper.jsonToMap(getRequestString());
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            String name = (String) body.get("name");

            if (email == null || password == null || name == null) {
                respondJson(400, JsonHelper.toJsonString(Map.of("error", "name, email et password requis")));
                return;
            }

            if (User.findFirst("email = ?", email) != null) {
                respondJson(409, JsonHelper.toJsonString(Map.of("error", "Email déjà utilisé")));
                return;
            }

            User user = new User();
            user.set("name", name);
            user.set("email", email.toLowerCase());
            user.set("password_hash", PasswordUtil.hashPassword(password));
            user.set("is_admin", false);

            if (!user.saveIt()) {
                respondJson(400, JsonHelper.toJsonString(user.errors()));
                return;
            }

            respondJson(201, userJson(user));
        });
    }

    @POST
    public void logout() {
        runAction(() -> {
            String sessionId = cookieValue("session_id");
            if (sessionId != null) {
                SessionManager.removeSession(sessionId);
            }
            Cookie cookie = new Cookie("session_id", "");
            cookie.setPath("/");
            cookie.setHttpOnly();
            cookie.setMaxAge(0);
            sendCookie(cookie);
            respondEmpty(204);
        });
    }

    @GET
    public void me() {
        runAction(() -> {
            User user = getCurrentUser();
            if (user == null) {
                respondEmpty(401);
                return;
            }
            respondJson(200, userJson(user));
        });
    }
}
