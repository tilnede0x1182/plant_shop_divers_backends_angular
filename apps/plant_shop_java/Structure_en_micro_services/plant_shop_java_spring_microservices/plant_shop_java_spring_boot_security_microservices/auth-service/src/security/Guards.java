package security;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import model.User;

/**
 * Classe utilitaire pour la vérification des autorisations.
 * Fournit des méthodes pour exiger une authentification ou un rôle admin.
 */
@Component
public class Guards {

    /**
     * Vérifie que l'utilisateur est authentifié.
     * @param user Utilisateur à vérifier
     * @throws ResponseStatusException 401 si non authentifié
     */
    public void requireAuth(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    /**
     * Vérifie que l'utilisateur est administrateur.
     * @param user Utilisateur à vérifier
     * @throws ResponseStatusException 401 si non authentifié, 403 si non admin
     */
    public void requireAdmin(User user) {
        requireAuth(user);
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    /**
     * Vérifie que l'utilisateur courant est administrateur.
     * @throws ResponseStatusException 401 si non authentifié, 403 si non admin
     */
    public void requireAdmin() {
        // Surcharge sans paramètre pour Spring Security
        User user = getCurrentUser();
        requireAdmin(user);
    }

    /**
     * Récupère l'utilisateur courant depuis le contexte Spring Security.
     * @return L'utilisateur authentifié
     * @throws ResponseStatusException 401 si non authentifié
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
     * Exige et retourne l'utilisateur courant authentifié.
     * @return L'utilisateur authentifié
     * @throws ResponseStatusException 401 si non authentifié
     */
    public User requireUser() {
        return getCurrentUser();
    }
}
