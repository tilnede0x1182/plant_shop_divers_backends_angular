package util;

/**
 * Record représentant une identité transférée.
 * @param userId Integer ID de l'utilisateur
 * @param admin boolean Statut administrateur
 */
public record ForwardedIdentity(Integer userId, boolean admin) {

    /**
     * Crée une identité anonyme.
     * @return ForwardedIdentity Identité anonyme
     */
    public static ForwardedIdentity anonymous() {
        return new ForwardedIdentity(null, false);
    }

    /**
     * Vérifie si l'utilisateur est authentifié.
     * @return boolean true si authentifié
     */
    public boolean authenticated() {
        return userId != null;
    }
}
