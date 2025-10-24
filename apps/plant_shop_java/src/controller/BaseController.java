package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;
import model.User;
import org.json.JSONException;
import org.json.JSONObject;
import repository.UserRepository;
import util.Request;

/**
 * Classe de base pour tous les contrôleurs.
 * Fournit des méthodes utilitaires partagées pour la gestion des requêtes,
 * des réponses, de l'authentification et du parsing JSON.
 */
public abstract class BaseController implements HttpHandler {

    protected final Connection db;
    private final UserRepository userRepoForAuth; // Utilisé uniquement pour l'authentification

    public BaseController(Connection db) {
        this.db = db;
        this.userRepoForAuth = new UserRepository(db);
    }

    /**
     * Lit le corps d'une requête et le retourne en tant que String.
     */
    protected String readBody(HttpExchange ex) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ex.getRequestBody(), "UTF-8"))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    /**
     * Parse le corps d'une requête en tant qu'objet JSONObject.
     * Gère les corps vides ou invalides.
     */
    protected JSONObject parseJsonBody(HttpExchange ex) throws IOException {
        String body = readBody(ex);
        if (body == null || body.trim().isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(body);
        } catch (JSONException e) {
            // Retourne un objet vide si le JSON est mal formé, ce qui permet des vérifications de présence de clé
            return new JSONObject();
        }
    }

    /**
     * Envoie une réponse avec un corps JSON.
     */
    protected void sendJsonResponse(HttpExchange ex, int code, String jsonBody) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = jsonBody.getBytes("UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
     * Envoie une réponse vide (sans corps).
     */
    protected void sendEmptyResponse(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }

    /**
     * Récupère l'utilisateur authentifié à partir du cookie de session.
     * Utilise la Map de sessions statique de AuthController.
     */
    protected User getAuthenticatedUser(HttpExchange ex) throws SQLException {
        // Accède à la map statique de sessions de AuthController
        Map<String, Integer> sessions = AuthController.getSessions();
        return Request.getUserFromSession(ex, this.userRepoForAuth, sessions);
    }

    /**
     * Gère les erreurs et envoie une réponse 500.
     */
    protected void handleError(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace(); // Important pour le débogage côté serveur
        String errorJson = "{\"error\":\"Erreur interne du serveur: " + e.getMessage() + "\"}";
        sendJsonResponse(ex, 500, errorJson);
    }
}
