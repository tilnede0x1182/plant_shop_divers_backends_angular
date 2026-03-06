package security;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import model.User;

/**
 * Bean pour la validation de l'authentification et des roles.
 */
@Component
public class Guards {

    /**
     * Exige qu'un utilisateur soit authentifie.
     *
     * @param user Utilisateur a verifier
     */
    public void requireAuth(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    /**
     * Exige qu'un utilisateur soit administrateur.
     *
     * @param user Utilisateur a verifier
     */
    public void requireAdmin(User user) {
        requireAuth(user);
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    /**
     * Exige qu'un utilisateur soit administrateur (depuis le contexte).
     */
    public void requireAdmin() {
        // Surcharge sans paramètre pour Spring Security
        User user = getCurrentUser();
        requireAdmin(user);
    }

    /**
     * Recupere l'utilisateur courant depuis le contexte Spring Security.
     *
     * @return Utilisateur courant
     */
    private User getCurrentUser() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    /**
     * Exige et retourne l'utilisateur courant.
     *
     * @return Utilisateur authentifie
     */
    public User requireUser() {
        return getCurrentUser();
    }
}
