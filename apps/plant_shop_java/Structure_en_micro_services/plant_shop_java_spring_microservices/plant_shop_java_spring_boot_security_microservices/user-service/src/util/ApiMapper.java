package util;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

/** Classe utilitaire pour la conversion des entites en JSON */
public final class ApiMapper {

    /** Constructeur prive (classe utilitaire) */
    private ApiMapper() {}

    /** Convertit un User en Map JSON */
    public static Map<String, Object> toUser(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.id);
        map.put("name", user.name);
        map.put("email", user.email);
        map.put("admin", user.isAdmin);
        map.put("createdAt", toIso(user.createdAt));
        return map;
    }

    /** Convertit un Timestamp en format ISO-8601 */
    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
}
