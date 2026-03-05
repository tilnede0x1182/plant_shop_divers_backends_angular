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
	 * 
	 * @param userId Integer L ID de l utilisateur (null si anonyme)
	 * @param admin boolean true si l utilisateur est administrateur
	 */
    private AuthContext(Integer userId, boolean admin) {
        this.userId = userId;
        this.admin = admin;
    }

    /**
	 * Crée un contexte d authentification anonyme.
	 * 
	 * @return AuthContext Le contexte anonyme
	 */
    public static AuthContext anonymous() {
        return new AuthContext(null, false);
    }

    /**
	 * Extrait le contexte d authentification depuis les en-têtes HTTP.
	 * 
	 * @param ex HttpExchange La requête HTTP
	 * @return AuthContext Le contexte extrait
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
	 * Vérifie si l utilisateur est authentifié.
	 * 
	 * @return boolean true si authentifié
	 */
    public boolean isAuthenticated() {
        return userId != null;
    }

    /**
	 * Retourne l ID de l utilisateur authentifié.
	 * 
	 * @return int L ID de l utilisateur
	 * @throws IllegalStateException Si l utilisateur n est pas authentifié
	 */
    public int userId() {
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return userId;
    }

    /**
	 * Vérifie si l utilisateur est administrateur.
	 * 
	 * @return boolean true si administrateur
	 */
    public boolean isAdmin() {
        return admin;
    }
}
