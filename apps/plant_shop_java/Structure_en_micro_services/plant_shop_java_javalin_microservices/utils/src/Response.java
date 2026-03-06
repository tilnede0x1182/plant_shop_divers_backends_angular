package util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Classe utilitaire pour simplifier l'envoi de réponses HTTP.
 * Centralise la configuration des headers et l'écriture du corps de la réponse.
 */
public final class Response {

    /**
     * Constructeur privé pour classe utilitaire.
     */
    private Response() {} // Empêche l'instanciation

    /**
     * Envoie une réponse avec un corps JSON.
     * @param ex L'objet HttpExchange.
     * @param code Le code de statut HTTP (ex: 200, 404).
     * @param jsonBody Le corps de la réponse au format String JSON.
     * @throws IOException
     */
    public static void send(HttpExchange ex, int code, String jsonBody) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, jsonBody.getBytes("UTF-8").length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(jsonBody.getBytes("UTF-8"));
        }
        ex.close();
    }

    /**
     * Envoie une réponse vide (sans corps).
     * Utile pour les codes de statut comme 204 No Content.
     * @param ex L'objet HttpExchange.
     * @param code Le code de statut HTTP.
     * @throws IOException
     */
    public static void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }
}
