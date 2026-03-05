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
     * Vérifie si une origine est autorisée.
     * @param origin L'origine de la requête
     * @return true si l'origine est dans la liste blanche
     */
    public boolean isAllowed(String origin) {
        return origin != null && ALLOWED.contains(origin);
    }

    /**
     * Vérifie si la requête est un preflight CORS.
     * @param request La requête HTTP
     * @return true si c'est une requête OPTIONS
     */
    public boolean isPreflight(HttpRequest<?> request) {
        return "OPTIONS".equals(request.getMethodName());
    }

    /**
     * Génère une réponse pour les requêtes preflight.
     * @param request La requête OPTIONS
     * @param origin L'origine de la requête
     * @return Réponse avec les headers CORS appropriés
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
     * @param response La réponse à modifier
     * @param origin L'origine de la requête
     * @param request La requête HTTP
     * @return La réponse avec les headers CORS
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
