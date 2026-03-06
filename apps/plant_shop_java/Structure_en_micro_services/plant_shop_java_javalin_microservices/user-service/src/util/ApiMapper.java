package util;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

/**
 * ApiMapper local pour user-service avec uniquement les méthodes nécessaires
 */
public final class ApiMapper {

    /**
     * Constructeur privé pour classe utilitaire.
     */
    private ApiMapper() {
        // utilitaire statique
    }

    /**
     * Convertit un utilisateur en Map JSON.
     * @param user Utilisateur à convertir
     * @return Map représentant le JSON
     */
    public static Map<String, Object> toUser(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.id);
        map.put("name", user.name);
        map.put("email", user.email);
        map.put("admin", user.isAdmin);
        map.put("createdAt", toIso(user.createdAt));
        return map;
    }

    /**
     * Convertit un Timestamp en chaîne ISO.
     * @param timestamp Timestamp à convertir
     * @return Chaîne ISO ou null
     */
    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
}