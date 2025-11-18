package core;

/**
 * Représente une cible de routage pour la Gateway.
 * Définit quel service contacter en fonction du préfixe de l'URL.
 *
 * @param name   Nom du service (ex: "catalog-service")
 * @param host   Hôte et port (ex: "http://localhost:6102")
 * @param prefix Préfixe de l'URI à intercepter (ex: "/api/plants")
 */
public record RouteTarget(String name, String host, String prefix) {

    public boolean matches(String uri) {
        return uri.startsWith(prefix);
    }

    public String resolveUrl(String uri) {
        // Construit l'URL complète vers le microservice
        // ex: /api/plants/1 -> http://localhost:6102/api/plants/1
        return host + uri;
    }
}
