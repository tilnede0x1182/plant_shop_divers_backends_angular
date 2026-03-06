package util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Envoi standardisé des réponses HTTP JSON.
 */
public final class Response {

    /**
 * Constructeur privé - classe utilitaire.
 */
private Response() {}

    /**
 * Envoie une réponse JSON.
 * @param ex Échange HTTP
 * @param code Code de statut
 * @param jsonBody Corps JSON
 */
public static void send(HttpExchange ex, int code, String jsonBody) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = jsonBody.getBytes("UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse vide.
	 * @param ex Échange HTTP
	 * @param code Code de statut
	 */
	public static void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }
}
