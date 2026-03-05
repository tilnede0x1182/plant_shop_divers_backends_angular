package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import model.User;

/**
 * Classe utilitaire pour les contrôles d'autorisation.
 * Fournit des méthodes pour vérifier l'authentification et les droits admin.
 */
public final class Guards {

    /**
     * Constructeur privé pour empêcher l'instanciation.
     */
    private Guards() {}

    /**
     * Vérifie qu'un utilisateur est authentifié.
     * @param request La requête HTTP
     * @return L'utilisateur authentifié
     * @throws HttpStatusException 401 si non authentifié
     */
    public static User requireUser(HttpRequest<?> request) {
        User user = request.getAttribute("user", User.class).orElse(null);
        if (user == null) {
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return user;
    }

    /**
     * Vérifie qu'un utilisateur est admin.
     * @param request La requête HTTP
     * @return L'utilisateur admin authentifié
     * @throws HttpStatusException 401 si non authentifié, 403 si non admin
     */
    public static User requireAdmin(HttpRequest<?> request) {
        User user = requireUser(request);
        if (!user.isAdmin) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }
}
