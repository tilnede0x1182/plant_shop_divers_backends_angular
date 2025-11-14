package security;

import jakarta.enterprise.context.RequestScoped;
import model.User;

/**
 * Un bean @RequestScoped qui contient l'utilisateur
 * authentifié pour la requête en cours.
 * Il est rempli par le SessionAuthFilter et lu par les Guards.
 */
@RequestScoped
public class AuthenticatedUser {
    private User user;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
