package security;

import jakarta.enterprise.context.RequestScoped;
import models.User;

/**
 * Un bean @RequestScoped qui contient l'utilisateur
 * authentifié pour la requête en cours.
 * Il est rempli par le SessionAuthFilter et lu par les Guards.
 */
@RequestScoped
public class AuthenticatedUser {
    private User user;

    /**
     * Récupère l'utilisateur authentifié.
     * @return Utilisateur ou null si non authentifié
     */
    public User getUser() {
        return user;
    }

    /**
     * Définit l'utilisateur authentifié.
     * @param user Utilisateur à stocker
     */
    public void setUser(User user) {
        this.user = user;
    }
}
