package util;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

/**
 * ApiMapper local pour auth-service avec uniquement les méthodes nécessaires
 */
public final class ApiMapper {

    /**
     * Constructeur privé (classe utilitaire).
     */
    private ApiMapper() {
        // utilitaire statique
    }

    /**
     * Convertit un User en Map pour l'API.
     * @param user Utilisateur à convertir
     * @return Map des propriétés
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
     * Convertit un Timestamp en chaîne ISO 8601.
     * @param timestamp Timestamp à convertir
     * @return Chaîne ISO ou null
     */
    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
}