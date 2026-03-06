package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaire pour le hachage de mots de passe.
 */
public final class PasswordUtil {

    /**
	 * Constructeur privé.
	 */
	private PasswordUtil() {}

    /**
	 * Hash un mot de passe.
	 * @param plainTextPassword Mot de passe en clair
	 * @return Hash BCrypt
	 */
	public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    /**
	 * Vérifie un mot de passe.
	 * @param plainTextPassword Mot de passe en clair
	 * @param hashedPassword Hash BCrypt
	 * @return true si correspondance
	 */
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
