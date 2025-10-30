// src/controllers/AuthController.java
package controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.sql.Connection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import model.User;
import org.json.JSONObject;
import repository.UserRepository;
import util.PasswordUtil;

public final class AuthController {

    private final UserRepository userRepo;
    private static final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    public AuthController(Connection db) {
        this.userRepo = new UserRepository(db);
    }

    public static Map<String, Integer> getSessions() {
        return sessions;
    }

    public void register(Context ctx) throws Exception {
        JSONObject body = new JSONObject(ctx.body());
        String name = body.getString("name");
        String email = body.getString("email");
        String password = body.getString("password");

        if (userRepo.findByEmailWithPassword(email) != null) {
            ctx.status(HttpStatus.CONFLICT).json("{\"error\":\"Cet email est déjà utilisé.\"}");
            return;
        }

        String hash = PasswordUtil.hashPassword(password);
        User newUser = new User(name, email, hash, false);
        userRepo.create(newUser);
        ctx.status(HttpStatus.CREATED).json("{\"message\":\"Utilisateur créé\"}");
    }

    public void login(Context ctx) throws Exception {
        JSONObject body = new JSONObject(ctx.body());
        String email = body.getString("email");
        String password = body.getString("password");

        User user = userRepo.findByEmailWithPassword(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.passwordHash)) {
            ctx.status(HttpStatus.UNAUTHORIZED).json("{\"error\":\"Identifiants invalides\"}");
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user.id);
        ctx.cookie("session_id", sessionId, 3600); // Expire dans 1 heure
        ctx.status(HttpStatus.CREATED).json(user);
    }

    public void logout(Context ctx) {
        String sessionId = ctx.cookie("session_id");
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
        ctx.removeCookie("session_id");
        ctx.status(HttpStatus.NO_CONTENT);
    }

    public void me(Context ctx) {
        User user = ctx.attribute("user");
        ctx.json(user);
    }
}
