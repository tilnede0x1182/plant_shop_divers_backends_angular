package util;

/**
 * Record representant l'identite d'un utilisateur propagee par la Gateway.
 *
 * @param userId ID de l'utilisateur (null si anonyme)
 * @param admin true si l'utilisateur est administrateur
 */
public record ForwardedIdentity(Integer userId, boolean admin) {

    /**
     * Cree une identite anonyme (non authentifiee).
     *
     * @return Identite anonyme
     */
    public static ForwardedIdentity anonymous() {
        return new ForwardedIdentity(null, false);
    }

    /**
     * Verifie si l'identite est authentifiee.
     *
     * @return true si authentifie
     */
    public boolean authenticated() {
        return userId != null;
    }
}
