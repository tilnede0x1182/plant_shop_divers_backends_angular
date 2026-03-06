package util;

/**
 * Holder ThreadLocal pour stocker l'identité de la requête courante.
 * Permet l'accès à l'identité depuis n'importe quel point du code.
 */
public final class ForwardedIdentityHolder {

    private static final ThreadLocal<ForwardedIdentity> CURRENT =
        ThreadLocal.withInitial(ForwardedIdentity::anonymous);

    /** Constructeur privé (classe utilitaire). */
    private ForwardedIdentityHolder() {}

    /**
     * Définit l'identité pour le thread courant.
     * @param identity ForwardedIdentity Identité à stocker
     */
    public static void set(ForwardedIdentity identity) {
        CURRENT.set(identity == null ? ForwardedIdentity.anonymous() : identity);
    }

    /**
     * Récupère l'identité du thread courant.
     * @return ForwardedIdentity Identité courante
     */
    public static ForwardedIdentity get() {
        return CURRENT.get();
    }

    /**
     * Efface l'identité du thread courant.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
