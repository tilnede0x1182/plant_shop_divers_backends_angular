/**
 * Contexte de session utilisateur.
 * @param authenticated true si authentifié
 * @param userId ID de l'utilisateur
 * @param admin true si administrateur
 */
public record SessionContext(boolean authenticated, int userId, boolean admin) {
    /**
     * Crée un contexte de session anonyme.
     * @return Contexte non authentifié
     */
    public static SessionContext anonymous() {
        return new SessionContext(false, -1, false);
    }
}
