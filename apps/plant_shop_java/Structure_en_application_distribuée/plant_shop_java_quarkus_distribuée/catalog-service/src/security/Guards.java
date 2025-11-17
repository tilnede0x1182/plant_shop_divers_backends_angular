package catalog.security;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;

public final class Guards {

    private Guards() {}

    public static AuthContext.UserIdentity requireUser() {
        AuthContext.UserIdentity identity = AuthContext.get();
        if (identity == null) {
            throw new NotAuthorizedException("Authentification requise");
        }
        return identity;
    }

    public static AuthContext.UserIdentity requireAdmin() {
        AuthContext.UserIdentity identity = requireUser();
        if (!identity.admin()) {
            throw new ForbiddenException("Accès administrateur requis");
        }
        return identity;
    }
}
