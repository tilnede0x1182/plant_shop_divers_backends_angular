package util;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import model.User;
import org.json.JSONException;
import org.json.JSONObject;
import repository.UserRepository;

/**
 * Classe utilitaire pour simplifier le traitement des requêtes HTTP entrantes.
 */
public final class Request {

    private Request() {} // Empêche l'instanciation

    /**
     * Lit le corps d'une requête et le retourne en tant que String.
     */
    public static String read(HttpExchange ex) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ex.getRequestBody(), "UTF-8"))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    /**
     * Extrait une valeur d'un champ dans un corps JSON.
     * Utilise org.json pour un parsing plus robuste que les regex.
     * @return La valeur du champ, ou null si le champ n'existe pas ou si le JSON est invalide.
     */
    public static String getJsonField(String jsonBody, String key) {
        try {
            JSONObject json = new JSONObject(jsonBody);
            return json.optString(key, null);
        } catch (JSONException e) {
            return null; // Le corps n'est pas un JSON valide
        }
    }

    /**
     * Échappe les caractères spéciaux pour une inclusion sûre dans une chaîne JSON.
     */
    public static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

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
