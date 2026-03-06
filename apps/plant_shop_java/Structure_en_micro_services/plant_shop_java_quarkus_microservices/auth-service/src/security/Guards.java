package security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import models.User;

/**
 * Bean @RequestScoped qui fournit des méthodes pour
 * valider l'authentification et les rôles de l'utilisateur
 * stocké dans le bean AuthenticatedUser.
 */
@RequestScoped
public class Guards {

    @Inject
    AuthenticatedUser authenticatedUser;

    /**
     * Exige qu'un utilisateur soit authentifié.
     *
     * @return L'utilisateur authentifié
     * @throws WebApplicationException 401 si non authentifié
     */
    public User requireUser() {
        // Dans ce service (auth-service), le ForwardedIdentityHolder est lu par
        // le filtre du service, mais AuthController utilise sa propre session locale.
        // Ce Guards doit être utilisé pour /auth/me et les autres services.
        User user = authenticatedUser.getUser();
        if (user == null) {
            // Lance une exception JAX-RS qui se traduit par une réponse 401
            throw new WebApplicationException("Authentification requise", Response.Status.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * Exige qu'un utilisateur soit authentifié et administrateur.
     *
     * @return L'utilisateur administrateur authentifié
     * @throws WebApplicationException 401 si non authentifié, 403 si non admin
     */
    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            // Lance une exception JAX-RS qui se traduit par une réponse 403
            throw new WebApplicationException("Accès administrateur requis", Response.Status.FORBIDDEN);
        }
        return user;
    }

}
