package util;

import com.sun.net.httpserver.HttpExchange;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import model.User;
import repository.UserRepository;

/**
 * Classe utilitaire pour simplifier le traitement des requêtes HTTP entrantes.
 */
public final class Request {

    private Request() {} // Empêche l'instanciation

    /**
     * Récupère l'utilisateur authentifié à partir du cookie de session.
     * @param ex L'objet HttpExchange.
     * @param userRepo Le repository pour accéder aux données des utilisateurs.
     * @param sessions Le cache des sessions actives.
     * @return L'objet User correspondant à la session, ou null si non authentifié.
     */
    public static User getUserFromSession(HttpExchange ex, UserRepository userRepo, Map<String, Integer> sessions) throws SQLException {
        String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null) {
            return null;
        }

        Optional<String> sessionIdOpt = Stream.of(cookieHeader.split(";"))
            .map(String::trim)
            .filter(cookie -> cookie.startsWith("session_id="))
            .map(cookie -> cookie.substring("session_id=".length()))
            .findFirst();

        if (sessionIdOpt.isEmpty()) {
            return null;
        }

        String sessionId = sessionIdOpt.get();
        Integer userId = sessions.get(sessionId);

        if (userId == null) {
            return null;
        }

        return userRepo.find(userId);
    }
}
