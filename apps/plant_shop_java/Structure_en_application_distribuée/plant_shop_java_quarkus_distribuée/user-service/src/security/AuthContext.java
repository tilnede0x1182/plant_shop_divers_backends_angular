package user.security;

import model.User;

/**
 * Conserve l'utilisateur authentifié dans le Thread courant.
 */
public final class AuthContext {

    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(User user) {
        CURRENT.set(user);
    }

    public static User get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
