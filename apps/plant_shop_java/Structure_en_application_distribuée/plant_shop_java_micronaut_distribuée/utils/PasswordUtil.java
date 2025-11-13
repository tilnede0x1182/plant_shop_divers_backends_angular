package util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {}

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
