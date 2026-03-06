/**
 * Record representant la cible d'une route (service et chemin).
 *
 * @param service Nom du microservice cible
 * @param path Chemin de la requete
 */
public record RouteTarget(String service, String path) {
    /**
     * Resout le service cible a partir du chemin de la requete.
     *
     * @param path Chemin de la requete
     * @return RouteTarget ou null si route inconnue
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
