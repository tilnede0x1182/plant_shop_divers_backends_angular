package controller;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.Connection;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import model.User;
import repository.UserRepository;
import util.PasswordUtil;
import util.Request;
import util.Response;

public final class AuthController extends BaseController {

    private final UserRepository userRepo;
    // Un simple cache en mémoire pour les sessions. Dans une vraie application,
    // on utiliserait une base de données comme Redis.
    private static final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    public AuthController(Connection db) {
        this.userRepo = new UserRepository(db);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        // Le préfixe /api/auth est géré par le routeur, on ne reçoit que la fin du chemin
        String path = ex.getRequestURI().getPath().substring("/api/auth".length());

        try {
            if ("POST".equals(ex.getRequestMethod())) {
                if ("/register".equals(path)) {
                    register(ex);
                    return;
                }
                if ("/login".equals(path)) {
                    login(ex);
                    return;
                }
            }
            if ("GET".equals(ex.getRequestMethod())) {
                if ("/me".equals(path)) {
                    me(ex);
                    return;
                }
            }
            Response.send(ex, 404, "{\"error\":\"Route non trouvée dans AuthController\"}");
        } catch (Exception e) {
            e.printStackTrace();
            Response.send(ex, 500, "{\"error\":\"Erreur interne du serveur: " + e.getMessage() + "\"}");
        }
    }

    private void register(HttpExchange ex) throws Exception {
        String body = Request.read(ex);
        String name = Request.getJsonField(body, "name");
        String email = Request.getJsonField(body, "email");
        String password = Request.getJsonField(body, "password");

        if (name == null || email == null || password == null) {
            Response.send(ex, 400, "{\"error\":\"Les champs 'name', 'email' et 'password' sont requis.\"}");
            return;
        }

        if (userRepo.findByEmailWithPassword(email) != null) {
            Response.send(ex, 409, "{\"error\":\"Cet email est déjà utilisé.\"}");
            return;
        }

        String passwordHash = PasswordUtil.hashPassword(password);
        User newUser = new User(name, email, passwordHash, false); // Par défaut, non-admin

        int userId = userRepo.create(newUser);
        newUser.id = userId;

        // On ne connecte pas l'utilisateur automatiquement après l'inscription pour garder les choses simples
        Response.send(ex, 201, "{\"message\":\"Utilisateur créé avec succès.\", \"userId\":" + userId + "}");
    }

    private void login(HttpExchange ex) throws Exception {
        String body = Request.read(ex);
        String email = Request.getJsonField(body, "email");
        String password = Request.getJsonField(body, "password");

        if (email == null || password == null) {
            Response.send(ex, 400, "{\"error\":\"Les champs 'email' et 'password' sont requis.\"}");
            return;
        }

        User user = userRepo.findByEmailWithPassword(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.passwordHash)) {
            Response.send(ex, 401, "{\"error\":\"Email ou mot de passe incorrect.\"}");
            return;
        }

        // Créer une session
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user.id);

        // Envoyer le cookie de session au client
        ex.getResponseHeaders().add("Set-Cookie", "session_id=" + sessionId + "; HttpOnly; Path=/; Max-Age=3600");
        Response.send(ex, 201, "{\"message\":\"Connexion réussie.\"}");
    }

    private void me(HttpExchange ex) throws Exception {
        User currentUser = Request.getUserFromSession(ex, userRepo, sessions);

        if (currentUser == null) {
            Response.send(ex, 401, "{\"error\":\"Non authentifié.\"}");
            return;
        }

        // Renvoyer les informations de l'utilisateur (sans le mot de passe)
        String jsonResponse = String.format(
            "{\"id\":%d, \"name\":\"%s\", \"email\":\"%s\", \"isAdmin\":%b}",
            currentUser.id,
            Request.escapeJson(currentUser.name),
            Request.escapeJson(currentUser.email),
            currentUser.isAdmin
        );
        Response.send(ex, 200, jsonResponse);
    }
}
