package util;

import com.sun.net.httpserver.HttpExchange;
import io.micronaut.http.HttpRequest;

/**
 * Représente l'identité propagée par la gateway vers les microservices.
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

    public static AuthContext fromHeaders(HttpExchange ex) {
        return fromRawHeaders(
            ex.getRequestHeaders().getFirst("X-User-Id"),
            ex.getRequestHeaders().getFirst("X-User-Admin")
        );
    }

    public static AuthContext fromHeaders(HttpRequest<?> request) {
        return fromRawHeaders(
            request.getHeaders().get("X-User-Id"),
            request.getHeaders().get("X-User-Admin")
        );
    }

    private static AuthContext fromRawHeaders(String idHeader, String adminHeader) {
        if (idHeader == null || idHeader.isBlank()) {
            return anonymous();
        }
        try {
            int id = Integer.parseInt(idHeader.trim());
            boolean isAdmin = adminHeader != null && Boolean.parseBoolean(adminHeader.trim());
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
