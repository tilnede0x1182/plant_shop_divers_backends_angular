package auth.util;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

/**
 * Mapper pour convertir les objets en Map JSON.
 */
public final class ApiMapper {

    /**
	 * Constructeur privé.
	 */
	private ApiMapper() {}

    /**
	 * Convertit un utilisateur en Map.
	 * @param user L'utilisateur
	 * @return La Map
	 */
	public static Map<String, Object> toUser(User user) {
        Map<String, Object> map = base();
        map.put("id", user.id);
        map.put("name", user.name);
        map.put("email", user.email);
        map.put("admin", user.isAdmin);
        map.put("createdAt", toIso(user.createdAt));
        return map;
    }

    /**
	 * Convertit un Timestamp en ISO.
	 * @param timestamp Le timestamp
	 * @return La chaîne ISO
	 */
	private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }

    /**
	 * Crée une Map de base.
	 * @return La Map
	 */
	private static Map<String, Object> base() {
        return new LinkedHashMap<>();
    }
}