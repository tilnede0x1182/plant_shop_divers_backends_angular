package util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Envoi standardisé des réponses HTTP JSON.
 */
public final class Response {

    /** Constructeur prive (classe utilitaire) */
    private Response() {}

    /** Envoie une reponse JSON */
    public static void send(HttpExchange ex, int code, String jsonBody) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = jsonBody.getBytes("UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /** Envoie une reponse vide */
    public static void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }
}
