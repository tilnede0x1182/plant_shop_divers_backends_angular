package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitaires pour le hachage et la vérification de mots de passe avec BCrypt.
 */
public final class PasswordUtil {

    /**
	 * Constructeur privé pour empêcher l'instanciation.
	 */
	private PasswordUtil() {}

    /**
	 * Hache un mot de passe en clair avec BCrypt.
	 * 
	 * @param plainTextPassword String Le mot de passe en clair
	 * @return String Le hash BCrypt du mot de passe
	 */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    /**
	 * Vérifie qu un mot de passe en clair correspond à un hash BCrypt.
	 * 
	 * @param plainTextPassword String Le mot de passe en clair
	 * @param hashedPassword String Le hash BCrypt à vérifier
	 * @return boolean true si le mot de passe correspond, false sinon
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
