package services;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import models.Plant;
import util.EnvLoader;
import util.ForwardedIdentity;
import util.ForwardedIdentityHolder;
import util.JsonMapper;

/**
 * Client HTTP minimal vers catalog-service.
 * Centralise la construction des requêtes pour éviter de dupliquer la logique.
 */
@ApplicationScoped
public class CatalogClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    /**
     * Constructeur par defaut. Charge la configuration depuis l'environnement.
     */
    public CatalogClient() {
        this(EnvLoader.load());
    }

    /**
     * Constructeur avec configuration explicite.
     *
     * @param env Map des variables d'environnement
     */
    CatalogClient(Map<String, String> env) {
        this.baseUrl = normalize(resolveBaseUrl(env));
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    }

    /**
     * Recupere une plante depuis le service de catalogue.
     *
     * @param plantId ID de la plante
     * @return Objet Plant ou null si non trouvee
     * @throws Exception En cas d'erreur HTTP
     */
    public Plant fetchPlant(int plantId) throws Exception {
        HttpRequest request = request("/api/plants/" + plantId)
            .GET()
            .build();

        HttpResponse<String> response = send(request);

        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return JsonMapper.read(response.body(), Plant.class);
        }
        throw new IllegalStateException("Catalog service a renvoyé " + response.statusCode() + " sur GET /api/plants/" + plantId);
    }

    /**
     * Met a jour le stock d'une plante via le service de catalogue.
     *
     * @param plantId ID de la plante
     * @param newStock Nouvelle valeur du stock
     * @throws Exception En cas d'erreur HTTP
     */
    public void updateStock(int plantId, int newStock) throws Exception {
        Map<String, Integer> payload = Map.of("stock", newStock);
        HttpRequest request = request("/internal/plants/" + plantId + "/stock")
            .header("Content-Type", "application/json")
            .method("PATCH", BodyPublishers.ofString(JsonMapper.stringify(payload)))
            .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Echec de mise à jour du stock (" + response.statusCode() + ")");
        }
    }

    /**
     * Envoie une requete HTTP.
     *
     * @param request Requete a envoyer
     * @return Reponse HTTP
     * @throws Exception En cas d'erreur
     */
    private HttpResponse<String> send(HttpRequest request) throws Exception {
        try {
            return httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw ie;
        }
    }

    private HttpRequest.Builder request(String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + normalizedPath))
            .header("Accept", "application/json");

        ForwardedIdentity identity = ForwardedIdentityHolder.get();
        if (identity != null && identity.authenticated()) {
            builder.header("X-User-Id", String.valueOf(identity.userId()));
            builder.header("X-User-Admin", String.valueOf(identity.admin()));
        }
        return builder;
    }

    /**
     * Resout l'URL de base du service de catalogue.
     *
     * @param env Map des variables d'environnement
     * @return URL de base
     */
    private static String resolveBaseUrl(Map<String, String> env) {
        String explicit = env.get("CATALOG_SERVICE_URL");
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        String scheme = env.getOrDefault("CATALOG_SERVICE_SCHEME", "http");
        String host = env.getOrDefault("CATALOG_SERVICE_HOST", "localhost");
        String port = env.getOrDefault("CATALOG_SERVICE_PORT", "6102");
        return scheme + "://" + host + ":" + port;
    }

    /**
     * Normalise une URL en supprimant le slash final.
     *
     * @param url URL a normaliser
     * @return URL normalisee
     */
    private static String normalize(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
