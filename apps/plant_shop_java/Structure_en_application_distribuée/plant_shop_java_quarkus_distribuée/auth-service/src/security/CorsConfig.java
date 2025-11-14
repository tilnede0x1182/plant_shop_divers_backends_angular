package auth.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Set;

/**
 * Filtre JAX-RS pour gérer la configuration CORS.
 * @Provider l'enregistre automatiquement auprès de Quarkus.
 */
@Provider
public final class CorsConfig implements ContainerRequestFilter, ContainerResponseFilter {

    // L'Angular est sur 8300 (vu dans le test) ou 4200 (défaut) ?
    // Gardons 8300, 4200 (défaut Angular) et 3000 (défaut React) pour être larges.
    private static final Set<String> ALLOWED = Set.of(
        "http://localhost:8300",
        "http://127.0.0.1:8300"
    );

    private boolean isAllowed(String origin) {
        return origin != null && ALLOWED.contains(origin);
    }

    /**
     * Filtre de requête : intercepte les requêtes OPTIONS (preflight).
     */
    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        String origin = request.getHeaderString("Origin");
        if (isAllowed(origin) && "OPTIONS".equals(request.getMethod())) {
            Response response = Response.ok()
                .header("Access-Control-Allow-Origin", origin)
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers", "Content-Type, Cookie")
                .build();
            // Court-circuite la requête avec une réponse 200 OK
            request.abortWith(response);
        }
    }

    /**
     * Filtre de réponse : ajoute les en-têtes CORS à toutes les réponses sortantes.
     */
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        String origin = request.getHeaderString("Origin");
        if (isAllowed(origin)) {
            response.getHeaders().putSingle("Access-Control-Allow-Origin", origin);
            response.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
            response.getHeaders().putSingle("Vary", "Origin");
        }
    }
}
