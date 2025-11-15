package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import model.UserDTO;

/**
 * Guards local pour user-service
 * Lit les headers X-User-Id et X-User-Admin propagés par la gateway
 */
public final class Guards {

    private Guards() {}

    public static UserDTO requireUser(HttpRequest<?> request) {
        String userIdHeader = request.getHeaders().get("X-User-Id");
        String adminHeader = request.getHeaders().get("X-User-Admin");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }

        try {
            int userId = Integer.parseInt(userIdHeader);
            boolean isAdmin = "true".equalsIgnoreCase(adminHeader);
            return new UserDTO(userId, isAdmin);
        } catch (NumberFormatException e) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "X-User-Id invalide");
        }
    }

    public static UserDTO requireAdmin(HttpRequest<?> request) {
        UserDTO user = requireUser(request);
        if (!user.isAdmin) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }
}
