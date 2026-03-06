package security;

import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import util.ForwardedIdentity;
import util.ForwardedIdentityHolder;

/**
 * Classe utilitaire pour la vérification des autorisations.
 * Extrait l'identité depuis les headers forwarded par le gateway.
 */
@Component
public class Guards {

    /**
     * Exige et retourne l'utilisateur authentifié.
     * @return L'utilisateur courant
     * @throws ResponseStatusException 401 si non authentifié
     */
    public User requireUser() {
        ForwardedIdentity identity = ForwardedIdentityHolder.get();
        if (!identity.authenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return toUser(identity);
    }

    /**
     * Exige un utilisateur avec droits administrateur.
     * @return L'utilisateur admin
     * @throws ResponseStatusException 401 si non authentifié, 403 si non admin
     */
    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }

    /**
     * Convertit une identité forwardée en objet User.
     * @param identity Identité extraite des headers
     * @return Utilisateur avec id et statut admin
     */
    private User toUser(ForwardedIdentity identity) {
        return new User(
            identity.userId(),
            null,
            null,
            null,
            identity.admin(),
            null
        );
    }
}
