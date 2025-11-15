package security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * Guards local pour catalog-service
 * Lit les headers X-User-Id et X-User-Admin propagés par la gateway
 */
@RequestScoped
public class Guards {

    @Inject
    HttpHeaders headers;

    public void requireAdmin() {
        String userIdHeader = headers.getHeaderString("X-User-Id");
        String adminHeader = headers.getHeaderString("X-User-Admin");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new WebApplicationException("Authentification requise", Response.Status.UNAUTHORIZED);
        }

        boolean isAdmin = "true".equalsIgnoreCase(adminHeader);
        if (!isAdmin) {
            throw new WebApplicationException("Accès administrateur requis", Response.Status.FORBIDDEN);
        }
    }
}
