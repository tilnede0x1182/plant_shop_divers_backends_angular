package util;

import org.mindrot.jbcrypt.BCrypt;

/** Utilitaire pour le hachage et la vérification des mots de passe. */
public final class PasswordUtil {

    /** Constructeur privé pour empêcher l'instanciation. */
    private PasswordUtil() {}

    /** Hache un mot de passe en utilisant BCrypt. */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt());
    }

    /** Vérifie si un mot de passe correspond à son hash BCrypt. */
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
