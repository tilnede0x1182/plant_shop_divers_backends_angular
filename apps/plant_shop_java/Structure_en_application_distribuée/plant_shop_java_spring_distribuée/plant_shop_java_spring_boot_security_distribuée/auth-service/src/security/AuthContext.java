package security;

import model.User;

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
     * Définit l'utilisateur courant.
     * @param user Utilisateur à définir
     */
    public static void set(User user) {
        CURRENT.set(user);
    }

    /**
     * Retourne l'utilisateur courant.
     * @return Utilisateur ou null
     */
    public static User get() {
        return CURRENT.get();
    }

    /**
     * Efface l'utilisateur courant.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
