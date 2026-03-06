package util;

/**
 * Holder ThreadLocal pour l'identite propagee par la Gateway.
 * Permet aux Guards d'acceder a l'identite sans passer par le contexte HTTP.
 */
public final class ForwardedIdentityHolder {

    private static final ThreadLocal<ForwardedIdentity> CURRENT =
        ThreadLocal.withInitial(ForwardedIdentity::anonymous);

    /** Constructeur prive pour empecher l'instanciation. */
    private ForwardedIdentityHolder() {}

    /**
     * Definit l'identite pour le thread courant.
     *
     * @param identity Identite a stocker
     */
    public static void set(ForwardedIdentity identity) {
        CURRENT.set(identity == null ? ForwardedIdentity.anonymous() : identity);
    }

    /**
     * Recupere l'identite du thread courant.
     *
     * @return Identite stockee ou anonyme si non definie
     */
    public static ForwardedIdentity get() {
        ForwardedIdentity identity = CURRENT.get();
        return identity == null ? ForwardedIdentity.anonymous() : identity;
    }

    /**
     * Supprime l'identite du thread courant.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
