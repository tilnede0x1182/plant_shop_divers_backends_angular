package util;

import io.javalin.http.Context;

/**
 * Représente l'identité propagée par la gateway.
 */
public final class AuthContext {

    private final Integer userId;
    private final boolean admin;

    private AuthContext(Integer userId, boolean admin) {
        this.userId = userId;
        this.admin = admin;
    }

    public static AuthContext anonymous() {
        return new AuthContext(null, false);
    }

    public static AuthContext fromHeaders(Context ctx) {
        return fromRawValues(ctx.header("X-User-Id"), ctx.header("X-User-Admin"));
    }

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

    public boolean isAuthenticated() {
        return userId != null;
    }

    public int userId() {
        if (userId == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        return userId;
    }

    public boolean isAdmin() {
        return admin;
    }
}
