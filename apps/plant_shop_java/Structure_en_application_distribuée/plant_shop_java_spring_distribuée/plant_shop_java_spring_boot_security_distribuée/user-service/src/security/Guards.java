package user.security;

import jakarta.servlet.http.HttpServletRequest;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import util.ForwardedIdentity;

@Component
public class Guards {

    private final HttpServletRequest request;

    @Autowired
    public Guards(HttpServletRequest request) {
        this.request = request;
    }

    public User requireUser() {
        ForwardedIdentity identity = resolveIdentity();
        if (!identity.authenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }
        return toUser(identity);
    }

    public User requireAdmin() {
        User user = requireUser();
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }

    private User toUser(ForwardedIdentity identity) {
        User user = new User();
        user.id = identity.userId();
        user.isAdmin = identity.admin();
        return user;
    }

    private ForwardedIdentity resolveIdentity() {
        String idHeader = request.getHeader("X-User-Id");
        if (idHeader == null || idHeader.isBlank()) {
            return ForwardedIdentity.anonymous();
        }
        try {
            int id = Integer.parseInt(idHeader.trim());
            boolean admin = Boolean.parseBoolean(request.getHeader("X-User-Admin"));
            return new ForwardedIdentity(id, admin);
        } catch (NumberFormatException e) {
            return ForwardedIdentity.anonymous();
        }
    }
}
