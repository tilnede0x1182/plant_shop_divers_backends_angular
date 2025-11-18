package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaire minimal de hachage pour la seed distribuée.
 * On reste autonome (pas de dépendance jBCrypt) pour éviter les soucis de classpath.
 */
public final class PasswordUtil {

    private PasswordUtil() {}

    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null) {
            throw new IllegalArgumentException("plainTextPassword ne doit pas être null");
        }
        return sha256(plainTextPassword);
    }

    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        return sha256(plainTextPassword).equals(hashedPassword);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
