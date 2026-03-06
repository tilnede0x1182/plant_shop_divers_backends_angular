package util;

import model.User;

/**
 * Contexte d'authentification basé sur ThreadLocal.
 * Stocke l'utilisateur courant pour la durée de la requête.
 */
public class AuthContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    /**
     * Définit l'utilisateur courant pour ce thread.
     * @param user Utilisateur à stocker
     */
    public static void setUser(User user) {
        currentUser.set(user);
    }

    /**
     * Récupère l'utilisateur courant.
     * @return L'utilisateur ou null si non défini
     */
    public static User getUser() {
        return currentUser.get();
    }

    /**
     * Nettoie le contexte d'authentification du thread.
     */
    public static void clear() {
        currentUser.remove();
    }
}
