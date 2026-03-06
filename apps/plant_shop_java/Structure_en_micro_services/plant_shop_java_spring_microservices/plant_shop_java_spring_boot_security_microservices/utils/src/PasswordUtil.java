package util;

import org.mindrot.jbcrypt.BCrypt;

/** Utilitaire de hachage de mots de passe avec BCrypt */
public final class PasswordUtil {

    /** Constructeur prive (classe utilitaire) */
    private PasswordUtil() {}

    /** Hache un mot de passe en clair */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    /** Verifie un mot de passe contre son hash */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainTextPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            System.err.println("Avertissement : hash BCrypt invalide.");
            return false;
        }
    }
}
