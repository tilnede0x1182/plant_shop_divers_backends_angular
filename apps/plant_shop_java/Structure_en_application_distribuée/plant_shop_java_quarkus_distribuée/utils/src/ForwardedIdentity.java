package util;

/**
 * Identité propagée par la gateway.
 */
public record ForwardedIdentity(Integer userId, boolean admin) {

    /**
 * Crée une identité anonyme.
 * @return Identité non authentifiée
 */
public static ForwardedIdentity anonymous() {
        return new ForwardedIdentity(null, false);
    }

    public boolean authenticated() {
        return userId != null;
    }
}
