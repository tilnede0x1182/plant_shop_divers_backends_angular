package util;

import model.User;

/** Contexte d'authentification stocke dans un ThreadLocal */
public class AuthContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    /** Definit l'utilisateur courant pour le thread */
    public static void setUser(User user) {
        currentUser.set(user);
    }

    /** Recupere l'utilisateur courant du thread */
    public static User getUser() {
        return currentUser.get();
    }

    /** Nettoie le contexte du thread */
    public static void clear() {
        currentUser.remove();
    }
}
