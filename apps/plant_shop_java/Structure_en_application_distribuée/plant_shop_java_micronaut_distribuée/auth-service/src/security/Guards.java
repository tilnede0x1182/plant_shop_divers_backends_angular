package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import model.User;

/**
 * Utilitaire de vérification d'autorisation.
 */
public final class Guards {

    /**
 * Constructeur privé - classe utilitaire.
 */
private Guards() {}

    /**
 * Exige un utilisateur authentifié.
 * @param request Requête HTTP
 * @return Utilisateur authentifié
 */
public static User requireUser(HttpRequest<?> request) {
        User user = request.getAttribute("user", User.class).orElse(null);
        if (user == null) {
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return user;
    }

    /**
 * Exige un administrateur.
 * @param request Requête HTTP
 * @return Utilisateur admin
 */
public static User requireAdmin(HttpRequest<?> request) {
        User user = requireUser(request);
        if (!user.isAdmin) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }
}
