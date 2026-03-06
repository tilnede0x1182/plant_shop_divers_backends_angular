package security;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import models.User;
import repositories.UserRepository;
import util.ForwardedIdentityHolder;

/**
 * Bean RequestScoped pour résoudre et stocker l'utilisateur courant
 * en lisant l'ID propagé par la Gateway via ForwardedIdentityHolder.
 * Remplace l'ancien filtre Micronaut.
 */
@RequestScoped
public class SessionAuthFilter {

    @Inject
    UserRepository userRepo;

    private User user;
    private boolean loaded = false;

    /**
     * Récupère l'utilisateur authentifié depuis la base de données.
     * Charge l'utilisateur une seule fois par requête (lazy loading).
     *
     * @return L'utilisateur authentifié ou null si non authentifié
     */
    public User getUser() {
        if (!loaded) {
            loaded = true;
            try {
                Integer userId = ForwardedIdentityHolder.get().userId();
                if (userId != null) {
                    this.user = userRepo.find(userId);
                }
            } catch (Exception e) {
                System.err.println("Erreur de chargement de l'utilisateur authentifié: " + e.getMessage());
            }
        }
        return user;
    }

    /**
     * Vérifie si l'utilisateur authentifié est administrateur.
     *
     * @return true si l'utilisateur est admin, false sinon
     */
    public boolean isAdmin() {
        return ForwardedIdentityHolder.get().admin();
    }
}
