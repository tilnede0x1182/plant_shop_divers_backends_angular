package security;

import models.User;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * Un bean @RequestScope qui contient l'utilisateur
 * authentifié pour la requête en cours.
 * Il est rempli par le SessionAuthFilter et lu par les Guards.
 */
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuthenticatedUser {
    private User user;

    /** Récupère l'utilisateur authentifié. */
    public User getUser() {
        return user;
    }

    /**
     * Définit l'utilisateur authentifié.
     *
     * @param user User L'utilisateur à définir
     */
    public void setUser(User user) {
        this.user = user;
    }
}
