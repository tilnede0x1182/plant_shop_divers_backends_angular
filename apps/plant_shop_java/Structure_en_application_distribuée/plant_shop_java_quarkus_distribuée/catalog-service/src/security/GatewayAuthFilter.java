package catalog.security;

import jakarta.ws.rs.ext.Provider;
import util.ForwardAuthFilter;

/**
 * Injecte l'identité propagée par la gateway dans le ThreadLocal du service catalogue.
 */
@Provider
public final class GatewayAuthFilter extends ForwardAuthFilter {

    @Override
    protected void storeAuth(ForwardedAuth auth) {
        AuthContext.set(new AuthContext.UserIdentity(auth.userId(), auth.admin()));
    }

    @Override
    protected void clearAuth() {
        AuthContext.clear();
    }
}
