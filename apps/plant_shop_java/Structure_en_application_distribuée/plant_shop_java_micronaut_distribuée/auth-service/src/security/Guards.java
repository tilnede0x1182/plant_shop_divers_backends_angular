package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import models.User;

public final class Guards {

    private Guards() {}

    public static User requireUser(HttpRequest<?> request) {
        User user = request.getAttribute("user", User.class).orElse(null);
        if (user == null) {
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return user;
    }

    public static User requireAdmin(HttpRequest<?> request) {
        User user = requireUser(request);
        if (!user.isAdmin) {
            throw new HttpStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }
}
