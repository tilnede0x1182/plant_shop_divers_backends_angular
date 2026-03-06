package util;

import com.sun.net.httpserver.HttpExchange;

/**
 * Représente l'identité propagée par la gateway vers les microservices.
 */
public final class AuthContext {

    private final Integer userId;
    private final boolean admin;

    /** Constructeur privé. */
    private AuthContext(Integer userId, boolean admin) {
        this.userId = userId;
        this.admin = admin;
    }

    /** Retourne un contexte anonyme (non authentifié). */
    public static AuthContext anonymous() {
        return new AuthContext(null, false);
    }

    /** Extrait le contexte d'authentification depuis les headers HTTP. */
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

    /** Vérifie si l'utilisateur est authentifié. */
    public boolean isAuthenticated() {
        return userId != null;
    }

    /** Retourne l'ID utilisateur (exception si non authentifié). */
    public int userId() {
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return userId;
    }

    /** Vérifie si l'utilisateur est administrateur. */
    public boolean isAdmin() {
        return admin;
    }
}
