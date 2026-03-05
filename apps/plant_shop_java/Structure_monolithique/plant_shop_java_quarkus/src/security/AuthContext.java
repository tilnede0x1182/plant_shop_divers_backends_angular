package security;

import models.User;

/**
 * Conserve l'utilisateur authentifié dans le Thread courant.
 */
public final class AuthContext {

    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    /**
     * Constructeur privé pour empêcher l'instanciation.
     */
    private AuthContext() {}

    /**
     * Définit l'utilisateur pour le thread courant.
     * @param user Utilisateur authentifié
     */
    public static void set(User user) {
        CURRENT.set(user);
    }

    /**
     * Récupère l'utilisateur du thread courant.
     * @return Utilisateur ou null
     */
    public static User get() {
        return CURRENT.get();
    }

    /**
     * Efface l'utilisateur du thread courant.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
