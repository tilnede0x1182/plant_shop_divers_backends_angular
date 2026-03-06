package security;

import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import util.ForwardedIdentity;
import util.ForwardedIdentityHolder;

/**
 * Gardes de sécurité pour l'authentification.
 */
@Component
public class Guards {

    /**
     * Exige un utilisateur authentifié.
     * @return User Utilisateur authentifié
     */
    public User requireUser() {
        ForwardedIdentity identity = ForwardedIdentityHolder.get();
        if (!identity.authenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return toUser(identity);
    }

    /**
     * Exige un administrateur.
     * @return User Utilisateur admin
     */
    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication required");
        }
        return user;
    }

    /**
     * Convertit une identité en User.
     * @param identity ForwardedIdentity Identité à convertir
     * @return User Utilisateur créé
     */
    private User toUser(ForwardedIdentity identity) {
        User user = new User();
        user.id = identity.userId();
        user.isAdmin = identity.admin();
        return user;
    }
}
