/**
 * Record representant le contexte de session d'un utilisateur.
 *
 * @param authenticated true si l'utilisateur est authentifie
 * @param userId ID de l'utilisateur (-1 si anonyme)
 * @param admin true si l'utilisateur est administrateur
 */
public record SessionContext(boolean authenticated, int userId, boolean admin) {
    /**
     * Cree un contexte de session anonyme (non authentifie).
     *
     * @return Contexte de session anonyme
     */
    public static SessionContext anonymous() {
        return new SessionContext(false, -1, false);
    }
}
