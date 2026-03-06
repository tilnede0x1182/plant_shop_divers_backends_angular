package util;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

/**
 * Mapper pour transformer les entités en réponses API.
 * Convertit les objets User en Map JSON.
 */
public final class ApiMapper {

    /** Constructeur privé (classe utilitaire). */
    private ApiMapper() {}

    /**
     * Convertit un User en Map pour l'API.
     * @param user User Utilisateur à convertir
     * @return Map<String, Object> Représentation JSON
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
     * Convertit un Timestamp en format ISO.
     * @param ts Timestamp Date à convertir
     * @return String Date ISO ou null
     */
    private static String toIso(Timestamp ts) {
        return (ts != null) ?
            ts.toInstant().atOffset(ZoneOffset.UTC).toString() : null;
    }
}
