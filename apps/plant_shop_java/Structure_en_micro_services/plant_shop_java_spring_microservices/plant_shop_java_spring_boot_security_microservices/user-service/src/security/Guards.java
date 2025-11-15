package security;

import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import model.User;

@Component
public class Guards {

    public void requireAuth(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    public void requireAdmin(User user) {
        requireAuth(user);
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }

    public void requireAdmin() {
        // Surcharge sans paramètre pour Spring Security
        User user = getCurrentUser();
        requireAdmin(user);
    }

    private User getCurrentUser() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    public User requireUser() {
        return getCurrentUser();
    }
}
