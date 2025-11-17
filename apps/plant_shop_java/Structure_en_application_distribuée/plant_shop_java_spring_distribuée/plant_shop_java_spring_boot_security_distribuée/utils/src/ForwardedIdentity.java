package util;

/**
 * Représente l'identité transmise par la gateway via les en-têtes X-User-*.
 */
public record ForwardedIdentity(Integer userId, boolean admin) {

    public static ForwardedIdentity anonymous() {
        return new ForwardedIdentity(null, false);
    }

    public boolean authenticated() {
        return userId != null;
    }
}
