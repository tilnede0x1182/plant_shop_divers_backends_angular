package util;

import io.javalin.http.Context;

/**
 * Représente l'identité propagée par la gateway.
 */
public final class AuthContext {

    private final Integer userId;
    private final boolean admin;

    /**
     * Constructeur privé.
     * @param userId ID de l'utilisateur ou null
     * @param admin true si admin
     */
    private AuthContext(Integer userId, boolean admin) {
        this.userId = userId;
        this.admin = admin;
    }

    /**
     * Retourne un contexte anonyme (non authentifié).
     * @return Contexte anonyme
     */
    public static AuthContext anonymous() {
        return new AuthContext(null, false);
    }

    /**
     * Crée un contexte depuis les headers HTTP.
     * @param ctx Contexte Javalin
     * @return Contexte d'authentification
     */
    public static AuthContext fromHeaders(Context ctx) {
        return fromRawValues(ctx.header("X-User-Id"), ctx.header("X-User-Admin"));
    }

    /**
     * Crée un contexte depuis les valeurs brutes des headers.
     * @param idHeader Valeur de X-User-Id
     * @param adminHeader Valeur de X-User-Admin
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
        } catch (NumberFormatException ex) {
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
     * @return ID de l'utilisateur
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
