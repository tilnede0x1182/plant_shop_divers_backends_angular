package security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import model.User;

/**
 * Bean @RequestScoped qui fournit des méthodes pour
 * valider l'authentification et les rôles de l'utilisateur
 * stocké dans le bean AuthenticatedUser.
 */
@RequestScoped
public class Guards {

    @Inject
    AuthenticatedUser authenticatedUser;

    public User requireUser() {
        if (authenticatedUser.getUser() == null) {
            // Lance une exception JAX-RS qui se traduit par une réponse 401
            throw new WebApplicationException("Authentification requise", Response.Status.UNAUTHORIZED);
        }
        return authenticatedUser.getUser();
    }

    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            // Lance une exception JAX-RS qui se traduit par une réponse 403
            throw new WebApplicationException("Accès administrateur requis", Response.Status.FORBIDDEN);
        }
        return user;
    }

}
