package auth.util;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

/**
 * Utilitaire de conversion modèles vers format API.
 */
public final class ApiMapper {

    /**
	 * Constructeur privé - classe utilitaire.
	 */
	private ApiMapper() {
        // utilitaire statique
    }

    /**
	 * Convertit un User en Map API.
	 * @param user Utilisateur à convertir
	 * @return Map pour API
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
	 * Convertit un timestamp en chaîne ISO.
	 * @param timestamp Timestamp à convertir
	 * @return Chaîne ISO ou null
	 */
	private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }

    /**
	 * Crée une Map de base ordonnée.
	 * @return LinkedHashMap vide
	 */
	private static Map<String, Object> base() {
        return new LinkedHashMap<>();
    }
}