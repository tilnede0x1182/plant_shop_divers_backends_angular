package security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import model.UserDTO;

/**
 * Guards local pour user-service
 * Lit les headers X-User-Id et X-User-Admin propagés par la gateway
 */
@RequestScoped
public class Guards {

    @Inject
    HttpHeaders headers;

    public UserDTO requireUser() {
        String userIdHeader = headers.getHeaderString("X-User-Id");
        String adminHeader = headers.getHeaderString("X-User-Admin");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new WebApplicationException("Authentification requise", Response.Status.UNAUTHORIZED);
        }

        try {
            int userId = Integer.parseInt(userIdHeader);
            boolean isAdmin = "true".equalsIgnoreCase(adminHeader);
            return new UserDTO(userId, isAdmin);
        } catch (NumberFormatException e) {
            throw new WebApplicationException("X-User-Id invalide", Response.Status.BAD_REQUEST);
        }
    }

    public UserDTO requireAdmin() {
        UserDTO user = requireUser();
        if (!user.isAdmin) {
            throw new WebApplicationException("Accès administrateur requis", Response.Status.FORBIDDEN);
        }
        return user;
    }
}
