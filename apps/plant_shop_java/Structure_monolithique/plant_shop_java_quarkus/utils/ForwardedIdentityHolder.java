package util;

/**
 * Holder ThreadLocal pour l'identité transférée.
 */
public final class ForwardedIdentityHolder {

    private static final ThreadLocal<ForwardedIdentity> CURRENT =
        ThreadLocal.withInitial(ForwardedIdentity::anonymous);

    /** Constructeur privé pour empêcher l'instanciation. */
    private ForwardedIdentityHolder() {}

    /**
     * Définit l'identité pour le thread courant.
     * @param identity ForwardedIdentity Identité à définir
     */
    public static void set(ForwardedIdentity identity) {
        CURRENT.set(identity == null ? ForwardedIdentity.anonymous() : identity);
    }

    /**
     * Récupère l'identité du thread courant.
     * @return ForwardedIdentity Identité actuelle
     */
    public static ForwardedIdentity get() {
        ForwardedIdentity identity = CURRENT.get();
        return identity == null ? ForwardedIdentity.anonymous() : identity;
    }

    /**
     * Efface l'identité du thread courant.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
