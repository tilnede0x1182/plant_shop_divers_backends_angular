package util;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

/**
 * Classe utilitaire pour mapper les entites en Maps JSON.
 */
public final class ApiMapper {

    /** Constructeur prive pour empecher l'instanciation. */
    private ApiMapper() {}

    /**
     * Convertit un User en Map pour serialisation JSON.
     *
     * @param user Utilisateur a convertir
     * @return Map representant l'utilisateur
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
     * Convertit un Timestamp en format ISO 8601.
     *
     * @param ts Timestamp a convertir
     * @return Chaine ISO ou null
     */
    private static String toIso(Timestamp ts) {
        return (ts != null) ?
            ts.toInstant().atOffset(ZoneOffset.UTC).toString() : null;
    }
}
