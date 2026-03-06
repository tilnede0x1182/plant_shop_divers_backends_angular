package security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import models.User;
import util.ForwardedIdentity;
import util.ForwardedIdentityHolder;

/**
 * Garde de sécurité pour vérifier les autorisations.
 */
@ApplicationScoped
public class Guards {

    /**
     * Exige un utilisateur authentifié.
     * @return L'utilisateur authentifié
     */
    public User requireUser() {
        ForwardedIdentity identity = ForwardedIdentityHolder.get();
        if (!identity.authenticated()) {
            throw new WebApplicationException("Authentication required", Response.Status.UNAUTHORIZED);
        }
        return toUser(identity);
    }

    /**
     * Exige un utilisateur administrateur.
     * @return L'utilisateur admin
     */
    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            throw new WebApplicationException("Admin access required", Response.Status.FORBIDDEN);
        }
        return user;
    }

    /**
     * Convertit une identité en User.
     * @param identity Identité à convertir
     * @return Utilisateur créé
     */
    private User toUser(ForwardedIdentity identity) {
        User user = new User();
        user.id = identity.userId();
        user.isAdmin = identity.admin();
        return user;
    }
}
