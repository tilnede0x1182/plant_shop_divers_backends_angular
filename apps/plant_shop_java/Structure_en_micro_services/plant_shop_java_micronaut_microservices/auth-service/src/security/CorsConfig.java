package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpResponse;
import jakarta.inject.Singleton;
import java.util.Set;

/**
 * Centralises CORS rules and header wiring.
 */
@Singleton
public final class CorsConfig {

    private static final Set<String> ALLOWED = Set.of(
        "http://localhost:8300",
        "http://127.0.0.1:8300"
    );

    /**
     * Vérifie si l'origine est autorisée.
     * @param origin Origine à vérifier
     * @return true si autorisée
     */
    public boolean isAllowed(String origin) {
        return origin != null && ALLOWED.contains(origin);
    }

    /**
     * Vérifie si la requête est un preflight CORS.
     * @param request Requête HTTP
     * @return true si OPTIONS
     */
    public boolean isPreflight(HttpRequest<?> request) {
        return "OPTIONS".equals(request.getMethodName());
    }

    /**
     * Génère une réponse preflight CORS.
     * @param request Requête HTTP
     * @param origin Origine de la requête
     * @return Réponse HTTP configurée
     */
    public MutableHttpResponse<?> preflight(HttpRequest<?> request, String origin) {
        MutableHttpResponse<?> response = HttpResponse.ok();
        apply(response, origin, request);
        response.header("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
        String requested = request.getHeaders().get("Access-Control-Request-Headers");
        response.header("Access-Control-Allow-Headers", requested != null ? requested : "Content-Type, Cookie");
        return response;
    }

    /**
     * Applique les headers CORS à une réponse.
     * @param response Réponse HTTP
     * @param origin Origine de la requête
     * @param request Requête HTTP
     * @return Réponse avec headers CORS
     */
    public <T> MutableHttpResponse<T> apply(MutableHttpResponse<T> response, String origin, HttpRequest<?> request) {
        if (!isAllowed(origin)) return response;
        response.header("Access-Control-Allow-Origin", origin);
        response.header("Access-Control-Allow-Credentials", "true");
        response.header("Vary", "Origin");
        if (isPreflight(request)) {
            response.header("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
            String requested = request.getHeaders().get("Access-Control-Request-Headers");
            response.header("Access-Control-Allow-Headers", requested != null ? requested : "Content-Type, Cookie");
        }
        return response;
    }
}
