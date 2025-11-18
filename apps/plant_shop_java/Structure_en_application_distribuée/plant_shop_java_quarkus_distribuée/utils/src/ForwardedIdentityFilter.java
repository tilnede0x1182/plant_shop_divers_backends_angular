package util;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Filtre JAX-RS pour lire les headers X-User-Id et X-User-Admin
 * propagés par la Gateway et initialiser le ThreadLocal ForwardedIdentityHolder.
 */
@Provider
@Priority(1) // Haute priorité pour s'assurer qu'il s'exécute avant les Guards
public class ForwardedIdentityFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        ForwardedIdentity identity = extractIdentity(requestContext);
        ForwardedIdentityHolder.set(identity);
        // La libération du ThreadLocal sera gérée par le contexte de requête CDI/Quarkus
    }

    private ForwardedIdentity extractIdentity(ContainerRequestContext requestContext) {
        String idHeader = requestContext.getHeaderString("X-User-Id");
        if (idHeader == null || idHeader.isBlank()) {
            return ForwardedIdentity.anonymous();
        }
        try {
            int id = Integer.parseInt(idHeader.trim());
            String adminHeader = requestContext.getHeaderString("X-User-Admin");
            boolean admin = Boolean.parseBoolean(adminHeader);
            return new ForwardedIdentity(id, admin);
        } catch (NumberFormatException e) {
            return ForwardedIdentity.anonymous();
        }
    }
}
