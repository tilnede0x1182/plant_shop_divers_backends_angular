package util;

import com.sun.net.httpserver.HttpExchange;

import java.util.Optional;
import java.util.stream.Stream;

public final class Request {

    private Request() {}

    public static String extractSessionId(HttpExchange ex) {
        String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return null;
        }

        Optional<String> sessionIdOpt = Stream.of(cookieHeader.split(";"))
            .map(String::trim)
            .filter(cookie -> cookie.startsWith("session_id="))
            .map(cookie -> cookie.substring("session_id=".length()))
            .findFirst();

        return sessionIdOpt.orElse(null);
    }
}
