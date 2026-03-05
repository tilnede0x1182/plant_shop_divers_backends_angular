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
 * Exige un utilisateur authentifié.
 * @return Utilisateur authentifié
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
 * Exige un administrateur.
 * @return Utilisateur admin
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
