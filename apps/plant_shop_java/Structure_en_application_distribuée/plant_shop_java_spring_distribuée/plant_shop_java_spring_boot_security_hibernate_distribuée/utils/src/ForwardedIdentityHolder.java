package util;

public final class ForwardedIdentityHolder {

    private static final ThreadLocal<ForwardedIdentity> CURRENT =
        ThreadLocal.withInitial(ForwardedIdentity::anonymous);

    private ForwardedIdentityHolder() {}

    public static void set(ForwardedIdentity identity) {
        CURRENT.set(identity == null ? ForwardedIdentity.anonymous() : identity);
    }

    public static ForwardedIdentity get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
