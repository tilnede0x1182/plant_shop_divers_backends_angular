package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaire de hachage de mots de passe BCrypt.
 */
public final class PasswordUtil {

    /**
 * Constructeur privé - classe utilitaire.
 */
private PasswordUtil() {}

    /**
 * Hash un mot de passe avec BCrypt.
 * @param plainTextPassword Mot de passe en clair
 * @return Hash BCrypt
 */
public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

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
