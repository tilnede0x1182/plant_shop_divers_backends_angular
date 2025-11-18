package util;

public record ForwardedIdentity(Integer userId, boolean admin) {

    public static ForwardedIdentity anonymous() {
        return new ForwardedIdentity(null, false);
    }

    public boolean authenticated() {
        return userId != null;
    }
}
