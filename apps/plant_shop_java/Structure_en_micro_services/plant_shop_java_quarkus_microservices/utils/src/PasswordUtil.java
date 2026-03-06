package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaire minimal de hachage pour la seed distribuée.
 * On reste autonome (pas de dépendance jBCrypt) pour éviter les soucis de classpath.
 */
public final class PasswordUtil {

    /** Constructeur prive pour empecher l'instanciation. */
    private PasswordUtil() {}

    /**
     * Hashe un mot de passe en clair.
     *
     * @param plainTextPassword Mot de passe en clair
     * @return Hash SHA-256 du mot de passe
     */
    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null) {
            throw new IllegalArgumentException("plainTextPassword ne doit pas être null");
        }
        return sha256(plainTextPassword);
    }

    /**
     * Verifie si un mot de passe correspond a un hash.
     *
     * @param plainTextPassword Mot de passe en clair
     * @param hashedPassword Hash a comparer
     * @return true si correspondance
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        if (plainTextPassword == null || hashedPassword == null) {
            return false;
        }
        return sha256(plainTextPassword).equals(hashedPassword);
    }

    /**
     * Calcule le hash SHA-256 d'une chaine.
     *
     * @param input Chaine a hasher
     * @return Hash en hexadecimal
     */
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /**
     * Convertit un tableau d'octets en chaine hexadecimale.
     *
     * @param data Tableau d'octets
     * @return Chaine hexadecimale
     */
    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
