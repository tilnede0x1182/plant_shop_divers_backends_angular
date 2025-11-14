package user.security;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import model.User;

public final class Guards {

    private Guards() {}

    public static User requireUser() {
        User user = AuthContext.get();
        if (user == null) {
            throw new NotAuthorizedException("Authentification requise");
        }
        return user;
    }

    public static User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            throw new ForbiddenException("Accès administrateur requis");
        }
        return user;
    }
}
