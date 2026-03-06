package security;

import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import util.ForwardedIdentity;
import util.ForwardedIdentityHolder;

@Component
/** Classe utilitaire pour les verifications de securite */
public class Guards {

    /** Verifie qu'un utilisateur est authentifie */
    public User requireUser() {
        ForwardedIdentity identity = ForwardedIdentityHolder.get();
        if (!identity.authenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return toUser(identity);
    }

    /** Verifie qu'un utilisateur est administrateur */
    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }

    /** Convertit une identite forwardee en objet User */
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
