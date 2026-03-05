package security;

import models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.http.HttpStatus;

/**
 * Classe utilitaire pour les contrôles d'autorisation.
 */
@Component
@RequestScope
public class Guards {

    @Autowired
    AuthenticatedUser authenticatedUser;

    /** Vérifie qu'un utilisateur est authentifié. */
    public User requireUser() {
        if (authenticatedUser.getUser() == null) {
            // Lance une exception que le GlobalExceptionHandler interceptera
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        User user = authenticatedUser.getUser();
        return authenticatedUser.getUser();
    }

    /** Vérifie qu'un utilisateur est admin. */
    public User requireAdmin() {
        User user = requireUser(); // Valide d'abord l'authentification
        if (!user.isAdmin) {
            // Lance une exception 403
            throw new AuthException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }
}
