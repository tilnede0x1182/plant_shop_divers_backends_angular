package util;

import com.sun.net.httpserver.HttpExchange;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utilitaires liés à la lecture des requêtes HTTP.
 */
public final class Request {

    private Request() {}

    /**
     * Extrait la valeur du cookie `session_id` si présent.
     */
    public static String extractSessionId(HttpExchange ex) {
        String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return null;
        }
        return Stream.of(cookieHeader.split(";"))
            .map(String::trim)
            .filter(cookie -> cookie.startsWith("session_id="))
            .map(cookie -> cookie.substring("session_id=".length()))
            .findFirst()
            .orElse(null);
    }
}
