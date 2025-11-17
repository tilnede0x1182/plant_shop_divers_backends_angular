package catalog.security;

/**
 * ThreadLocal contenant l'identité propagée par la gateway (id + admin).
 */
public final class AuthContext {

    private static final ThreadLocal<UserIdentity> CURRENT = new ThreadLocal<>();

    private AuthContext() {}

    public static void set(UserIdentity identity) {
        CURRENT.set(identity);
    }

    public static UserIdentity get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record UserIdentity(int id, boolean admin) {}
}
