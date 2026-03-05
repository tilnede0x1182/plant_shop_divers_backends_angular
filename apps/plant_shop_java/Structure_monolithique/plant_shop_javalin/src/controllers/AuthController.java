// src/controllers/AuthController.java
package controller;

import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import io.javalin.http.HttpStatus;
import java.sql.Connection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import model.User;
import org.json.JSONObject;
import repository.UserRepository;
import util.ApiMapper;
import util.PasswordUtil;

/**
 * Contrôleur d authentification.
 * Gère login, logout, register et me.
 */
public final class AuthController {

    private final UserRepository userRepo;
    private static final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    /**
     * Constructeur.
     * @param db Connection Connexion DB
     */
    public AuthController(Connection db) {
        this.userRepo = new UserRepository(db);
    }

    /**
     * Retourne le cache des sessions.
     * @return Map Sessions actives
     */
    public static Map<String, Integer> getSessions() {
        return sessions;
    }

    /**
     * Inscription d un nouvel utilisateur.
     * @param ctx Context Contexte Javalin
     * @throws Exception En cas d erreur
     */
    public void register(Context ctx) throws Exception {
        JSONObject body = new JSONObject(ctx.body());
        String name = body.getString("name");
        String email = body.getString("email");
        String password = body.getString("password");

        if (userRepo.findByEmailWithPassword(email) != null) {
            ctx.status(HttpStatus.CONFLICT).json(Map.of("error", "Cet email est déjà utilisé."));
            return;
        }

        String hash = PasswordUtil.hashPassword(password);
        User newUser = new User(name, email, hash, false);
        int newId = userRepo.create(newUser);
        User created = userRepo.find(newId);
        ctx.status(HttpStatus.CREATED).json(ApiMapper.toUser(created));
    }

    /**
     * Connexion d un utilisateur.
     * @param ctx Context Contexte Javalin
     * @throws Exception En cas d erreur
     */
    public void login(Context ctx) throws Exception {
        JSONObject body = new JSONObject(ctx.body());
        String email = body.getString("email");
        String password = body.getString("password");

        User user = userRepo.findByEmailWithPassword(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.passwordHash)) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Identifiants invalides"));
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user.id);
        Cookie cookie = new Cookie("session_id", sessionId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(3600);
        cookie.setSecure(false);
        cookie.setSameSite(SameSite.LAX);
        ctx.cookie(cookie);

        User sanitized = new User(user.id, user.name, user.email, null, user.isAdmin, user.createdAt);
        ctx.status(HttpStatus.CREATED).json(ApiMapper.toUser(sanitized));
    }

    /**
     * Déconnexion d un utilisateur.
     * @param ctx Context Contexte Javalin
     */
    public void logout(Context ctx) {
        String sessionId = ctx.cookie("session_id");
        if (sessionId != null) {
            sessions.remove(sessionId);
        }

        Cookie cookie = new Cookie("session_id", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setSameSite(SameSite.LAX);
        ctx.cookie(cookie);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    /**
     * Retourne l utilisateur connecté.
     * @param ctx Context Contexte Javalin
     */
    public void me(Context ctx) {
        User user = ctx.attribute("user");
        if (user == null) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Non authentifié"));
            return;
        }
        ctx.json(ApiMapper.toUser(user));
    }
}
