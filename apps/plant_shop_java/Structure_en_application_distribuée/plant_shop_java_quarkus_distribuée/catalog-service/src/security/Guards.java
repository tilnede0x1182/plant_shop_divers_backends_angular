package security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import models.User;
import util.ForwardedIdentity;
import util.ForwardedIdentityHolder;

@ApplicationScoped
public class Guards {

    public User requireUser() {
        ForwardedIdentity identity = ForwardedIdentityHolder.get();
        if (!identity.authenticated()) {
            throw new WebApplicationException("Authentication required", Response.Status.UNAUTHORIZED);
        }
        return toUser(identity);
    }

    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            throw new WebApplicationException("Admin access required", Response.Status.FORBIDDEN);
        }
        return user;
    }

    private User toUser(ForwardedIdentity identity) {
        User user = new User();
        user.id = identity.userId();
        user.isAdmin = identity.admin();
        return user;
    }
}
