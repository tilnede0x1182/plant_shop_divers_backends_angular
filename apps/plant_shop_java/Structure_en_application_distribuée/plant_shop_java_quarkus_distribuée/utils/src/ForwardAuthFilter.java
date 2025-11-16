package util;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

/**
 * Filtre utilitaire qui lit les en-têtes propagés par la gateway
 * (X-User-Id / X-User-Admin) et délègue le stockage à une sous-classe.
 * Permet d'éviter toute duplication de logique entre services.
 */
public abstract class ForwardAuthFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String KEY = ForwardAuthFilter.class.getName() + ".present";

    @Override
    public final void filter(ContainerRequestContext requestContext) throws IOException {
        clearAuth();
        ForwardedAuth forwarded = ForwardedAuth.from(requestContext);
        if (forwarded != null) {
            storeAuth(forwarded);
            requestContext.setProperty(KEY, Boolean.TRUE);
        }
    }

    @Override
    public final void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (requestContext.getProperty(KEY) != null) {
            clearAuth();
            requestContext.removeProperty(KEY);
        }
    }

    protected abstract void storeAuth(ForwardedAuth auth);

    protected abstract void clearAuth();

    /**
     * Représente l'identité propagée.
     */
    protected static final class ForwardedAuth {
        private final int userId;
        private final boolean admin;

        private ForwardedAuth(int userId, boolean admin) {
            this.userId = userId;
            this.admin = admin;
        }

        static ForwardedAuth from(ContainerRequestContext ctx) {
            String idHeader = ctx.getHeaderString("X-User-Id");
            if (idHeader == null || idHeader.isBlank()) {
                return null;
            }
            try {
                int id = Integer.parseInt(idHeader.trim());
                boolean admin = Boolean.parseBoolean(String.valueOf(ctx.getHeaderString("X-User-Admin")));
                return new ForwardedAuth(id, admin);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public int userId() {
            return userId;
        }

        public boolean admin() {
            return admin;
        }
    }
}
