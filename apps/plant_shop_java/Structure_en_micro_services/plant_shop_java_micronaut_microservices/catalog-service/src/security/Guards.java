package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;

/**
 * Guards local pour catalog-service
 * Lit les headers X-User-Id et X-User-Admin propagés par la gateway
 */
public final class Guards {

    private Guards() {}

    public static void requireAdmin(HttpRequest<?> request) {
        String userIdHeader = request.getHeaders().get("X-User-Id");
        String adminHeader = request.getHeaders().get("X-User-Admin");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }

        boolean isAdmin = "true".equalsIgnoreCase(adminHeader);
        if (!isAdmin) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
    }
}
