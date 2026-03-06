package security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // Doit s'exécuter avant tous les autres filtres
/**
 * Filtre CORS pour autoriser les requêtes cross-origin.
 */
public class CorsFilter implements Filter {

    // Origines autorisées (idem projet Quarkus)
    private static final Set<String> ALLOWED = Set.of(
        "http://localhost:8300",
        "http://127.0.0.1:8300"
    );

    /**
	 * Vérifie si l'origine est autorisée.
	 * @param origin String Origine de la requête
	 * @return boolean true si autorisée
	 */
    private boolean isAllowed(String origin) {
        return origin != null && ALLOWED.contains(origin);
    }

    /**
	 * Filtre les requêtes pour ajouter les en-têtes CORS.
	 * @param req ServletRequest Requête entrante
	 * @param res ServletResponse Réponse sortante
	 * @param chain FilterChain Chaîne de filtres
	 * @throws IOException En cas d'erreur I/O
	 * @throws ServletException En cas d'erreur servlet
	 */
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String origin = request.getHeader("Origin");

        if (isAllowed(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Cookie");
            response.setHeader("Vary", "Origin");
        }

        // Gère la requête preflight OPTIONS
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) && isAllowed(origin)) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            // Passe à la suite (SessionAuthFilter ou le contrôleur)
            chain.doFilter(req, res);
        }
    }
}
