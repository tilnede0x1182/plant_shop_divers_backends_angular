package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import model.UserDTO;

/**
 * Guards local pour order-service
 * Lit les headers X-User-Id et X-User-Admin propagés par la gateway
 */
public final class Guards {

    /**
     * Constructeur privé pour classe utilitaire.
     */
    private Guards() {}

    /**
     * Exige un utilisateur authentifié via les headers.
     * @param request Requête HTTP
     * @return DTO utilisateur
     * @throws HttpStatusException Si non authentifié
     */
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

    /**
     * Exige un administrateur authentifié via les headers.
     * @param request Requête HTTP
     * @return DTO utilisateur admin
     * @throws HttpStatusException Si non admin
     */
    public static UserDTO requireAdmin(HttpRequest<?> request) {
        UserDTO user = requireUser(request);
        if (!user.isAdmin) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }
}
