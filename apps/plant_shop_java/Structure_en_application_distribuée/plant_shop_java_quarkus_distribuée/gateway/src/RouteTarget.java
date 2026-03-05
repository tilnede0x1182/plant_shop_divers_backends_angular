/**
 * Record représentant une cible de routage.
 * @param service Nom du service cible
 * @param path Chemin de la requête
 */
public record RouteTarget(String service, String path) {
    /**
     * Résout le service cible depuis le chemin.
     * @param path Chemin de la requête
     * @return RouteTarget ou null si non trouvé
     */
    public static RouteTarget resolve(String path) {
        if (path.startsWith("/api/auth")) {
            return new RouteTarget("auth", path);
        }
        if (path.startsWith("/api/plants") || path.startsWith("/api/admin/plants")) {
            return new RouteTarget("catalog", path);
        }
        if (path.startsWith("/api/orders") || path.startsWith("/api/admin/orders")) {
            return new RouteTarget("order", path);
        }
        if (path.startsWith("/api/users") || path.startsWith("/api/admin/users")) {
            return new RouteTarget("user", path);
        }
        return null;
    }
}
