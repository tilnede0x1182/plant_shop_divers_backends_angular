package util;

/** Holder ThreadLocal pour l'identite forwardee */
public final class ForwardedIdentityHolder {

    private static final ThreadLocal<ForwardedIdentity> CURRENT =
        ThreadLocal.withInitial(ForwardedIdentity::anonymous);

    /** Constructeur prive (classe utilitaire) */
    private ForwardedIdentityHolder() {}

    /** Definit l'identite pour le thread courant */
    public static void set(ForwardedIdentity identity) {
        CURRENT.set(identity == null ? ForwardedIdentity.anonymous() : identity);
    }

    /** Recupere l'identite du thread courant */
    public static ForwardedIdentity get() {
        ForwardedIdentity identity = CURRENT.get();
        return identity == null ? ForwardedIdentity.anonymous() : identity;
    }

    /** Nettoie l'identite du thread courant */
    public static void clear() {
        CURRENT.remove();
    }
}
