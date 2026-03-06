package util;

import com.sun.net.httpserver.HttpExchange;

/**
 * Représente l'identité propagée par la gateway vers les microservices.
 */
public final class AuthContext {

    private final Integer userId;
    private final boolean admin;

    /**
	 * Constructeur privé.
	 * @param userId ID utilisateur
	 * @param admin Est administrateur
	 */
	private AuthContext(Integer userId, boolean admin) {
        this.userId = userId;
        this.admin = admin;
    }

    /**
	 * Crée un contexte anonyme.
	 * @return Contexte anonyme
	 */
	public static AuthContext anonymous() {
        return new AuthContext(null, false);
    }

    /**
	 * Crée un contexte depuis les headers HTTP.
	 * @param ex Échange HTTP
	 * @return Contexte d'authentification
	 */
	public static AuthContext fromHeaders(HttpExchange ex) {
        String idHeader = ex.getRequestHeaders().getFirst("X-User-Id");
        if (idHeader == null || idHeader.isBlank()) {
            return anonymous();
        }
        try {
            int id = Integer.parseInt(idHeader.trim());
            boolean isAdmin = Boolean.parseBoolean(
                ex.getRequestHeaders().getFirst("X-User-Admin"));
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
	 * Retourne l'ID utilisateur.
	 * @return ID utilisateur
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
