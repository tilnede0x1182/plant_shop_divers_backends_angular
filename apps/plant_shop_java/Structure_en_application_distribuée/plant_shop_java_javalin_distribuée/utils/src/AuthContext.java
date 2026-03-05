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

    public static AuthContext fromHeaders(HttpExchange ex) {
        return fromRawValues(
            ex.getRequestHeaders().getFirst("X-User-Id"),
            ex.getRequestHeaders().getFirst("X-User-Admin")
        );
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
        } catch (NumberFormatException exn) {
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
