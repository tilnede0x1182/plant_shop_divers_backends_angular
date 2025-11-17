package user.security;

import model.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import util.ForwardedIdentity;
import util.ForwardedIdentityHolder;

public final class Guards {

    private Guards() {}

    public static User requireUser() {
        ForwardedIdentity identity = ForwardedIdentityHolder.get();
        if (!identity.authenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return toUser(identity);
    }

    public static User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }

    private static User toUser(ForwardedIdentity identity) {
        User user = new User();
        user.id = identity.userId();
        user.isAdmin = identity.admin();
        return user;
    }
}
