package util;

/**
 * Record représentant l'identité transmise par le gateway.
 * Contient l'état d'authentification et les infos utilisateur.
 */
public record ForwardedIdentity(boolean authenticated, int userId, boolean admin) {

    /**
     * Crée une identité anonyme (non authentifiée).
     * @return ForwardedIdentity Identité anonyme
     */
    public static ForwardedIdentity anonymous() {
        return new ForwardedIdentity(false, -1, false);
    }
}
