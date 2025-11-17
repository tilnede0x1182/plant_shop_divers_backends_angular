package util;

public record ForwardedIdentity(boolean authenticated, int userId, boolean admin) {

    public static ForwardedIdentity anonymous() {
        return new ForwardedIdentity(false, -1, false);
    }
}
