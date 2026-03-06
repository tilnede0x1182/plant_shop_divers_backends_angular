package security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import models.User;
import util.ForwardedIdentity;
import util.ForwardedIdentityHolder;

/**
 * Bean singleton pour la validation de l'authentification et des rôles.
 * Utilise l'identité propagée par la Gateway.
 */
@ApplicationScoped
public class Guards {

    /**
     * Exige qu'un utilisateur soit authentifié.
     *
     * @return L'utilisateur authentifié
     * @throws WebApplicationException 401 si non authentifié
     */
    public User requireUser() {
        ForwardedIdentity identity = ForwardedIdentityHolder.get();
        if (!identity.authenticated()) {
            throw new WebApplicationException("Authentication required", Response.Status.UNAUTHORIZED);
        }
        return toUser(identity);
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
            throw new WebApplicationException("Admin access required", Response.Status.FORBIDDEN);
        }
        return user;
    }

    /**
     * Convertit une identité propagée en objet User.
     *
     * @param identity Identité propagée par la Gateway
     * @return Objet User avec l'ID et le statut admin
     */
    private User toUser(ForwardedIdentity identity) {
        User user = new User();
        user.id = identity.userId();
        user.isAdmin = identity.admin();
        return user;
    }
}
