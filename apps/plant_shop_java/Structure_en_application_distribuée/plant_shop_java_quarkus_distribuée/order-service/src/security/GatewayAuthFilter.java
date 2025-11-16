package order.security;

import jakarta.ws.rs.ext.Provider;
import model.User;
import util.ForwardAuthFilter;

/**
 * Injecte l'identité transmise par la gateway dans le ThreadLocal du service.
 */
@Provider
public final class GatewayAuthFilter extends ForwardAuthFilter {

    @Override
    protected void storeAuth(ForwardedAuth auth) {
        User user = new User();
        user.id = auth.userId();
        user.isAdmin = auth.admin();
        AuthContext.set(user);
    }

    @Override
    protected void clearAuth() {
        AuthContext.clear();
    }
}
