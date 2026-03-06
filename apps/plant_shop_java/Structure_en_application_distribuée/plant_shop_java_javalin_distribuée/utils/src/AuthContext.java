package util;

import com.sun.net.httpserver.HttpExchange;
import io.javalin.http.Context;

/**
 * Représente l'identité propagée par la gateway vers les microservices.
 */
public final class AuthContext {

    private final Integer userId;
    private final boolean admin;

    /**
 * Constructeur privé.
 * @param userId ID utilisateur ou null
 * @param admin Flag admin
 */
private AuthContext(Integer userId, boolean admin) {
        this.userId = userId;
        this.admin = admin;
    }

    /**
 * Crée un contexte anonyme.
 * @return Contexte non authentifié
 */
public static AuthContext anonymous() {
        return new AuthContext(null, false);
    }

    /**
	 * Extrait le contexte depuis les headers HttpExchange.
	 * @param ex Échange HTTP
	 * @return Contexte d'authentification
	 */
	public static AuthContext fromHeaders(HttpExchange ex) {
        return fromRawValues(
            ex.getRequestHeaders().getFirst("X-User-Id"),
            ex.getRequestHeaders().getFirst("X-User-Admin")
        );
    }

    /**
	 * Extrait le contexte depuis le contexte Javalin.
	 * @param ctx Contexte Javalin
	 * @return Contexte d'authentification
	 */
	public static AuthContext fromHeaders(Context ctx) {
        return fromRawValues(ctx.header("X-User-Id"), ctx.header("X-User-Admin"));
    }

    /**
	 * Construit un contexte depuis les valeurs brutes.
	 * @param idHeader Header X-User-Id
	 * @param adminHeader Header X-User-Admin
	 * @return Contexte d'authentification
	 */
	private static AuthContext fromRawValues(String idHeader, String adminHeader) {
        if (idHeader == null || idHeader.isBlank()) {
            return anonymous();
        }
        try {
            int id = Integer.parseInt(idHeader.trim());
            boolean isAdmin = Boolean.parseBoolean(adminHeader);
            return new AuthContext(id, isAdmin);
        } catch (NumberFormatException exn) {
            return anonymous();
        }
    }

    /**
	 * Vérifie si l'utilisateur est authentifié.
	 * @return true si authentifié
	 */
	public boolean isAuthenticated() {
        return userId != null;
    }

    /**
	 * Retourne l'ID de l'utilisateur.
	 * @return ID utilisateur
	 * @throws IllegalStateException si non authentifié
	 */
	public int userId() {
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return userId;
    }

    /**
	 * Vérifie si l'utilisateur est admin.
	 * @return true si admin
	 */
	public boolean isAdmin() {
        return admin;
    }
}
