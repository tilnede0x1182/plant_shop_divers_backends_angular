package util;

/** Record representant l'identite forwardee par la gateway */
public record ForwardedIdentity(Integer userId, boolean admin) {

    /** Cree une identite anonyme (non authentifiee) */
    public static ForwardedIdentity anonymous() {
        return new ForwardedIdentity(null, false);
    }

    /** Verifie si l'identite est authentifiee */
    public boolean authenticated() {
        return userId != null;
    }
}
