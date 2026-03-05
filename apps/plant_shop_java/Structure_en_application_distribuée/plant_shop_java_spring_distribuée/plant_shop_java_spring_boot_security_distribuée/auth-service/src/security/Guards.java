package security;

import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
/**
 * Garde de sécurité pour vérifier les autorisations.
 */
public class Guards {

    /**
     * Exige un utilisateur authentifié.
     * @return L'utilisateur authentifié
     */
    public User requireUser() {
        User user = AuthContext.get();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return user;
    }

    /**
     * Exige un utilisateur administrateur.
     * @return L'utilisateur admin
     */
    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }
}
