package util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
/**
 * Filtre HTTP pour extraire l'identité des headers X-User-*.
 * Stocke l'identité dans le ThreadLocal pour accès par les guards.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class ForwardedIdentityFilter extends OncePerRequestFilter {

    /**
     * Filtre chaque requête pour extraire l'identité.
     * @param request HttpServletRequest Requête entrante
     * @param response HttpServletResponse Réponse
     * @param filterChain FilterChain Chaîne de filtres
     * @throws ServletException En cas d'erreur servlet
     * @throws IOException En cas d'erreur I/O
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            ForwardedIdentityHolder.set(extractIdentity(request));
            filterChain.doFilter(request, response);
        } finally {
            ForwardedIdentityHolder.clear();
        }
    }

    /**
     * Extrait l'identité des headers de la requête.
     * @param request HttpServletRequest Requête
     * @return ForwardedIdentity Identité extraite
     */
    private ForwardedIdentity extractIdentity(HttpServletRequest request) {
        String idHeader = request.getHeader("X-User-Id");
        if (idHeader == null || idHeader.isBlank()) {
            return ForwardedIdentity.anonymous();
        }
        try {
            int id = Integer.parseInt(idHeader.trim());
            boolean admin = Boolean.parseBoolean(request.getHeader("X-User-Admin"));
            return new ForwardedIdentity(true, id, admin);
        } catch (NumberFormatException e) {
            return ForwardedIdentity.anonymous();
        }
    }
}
