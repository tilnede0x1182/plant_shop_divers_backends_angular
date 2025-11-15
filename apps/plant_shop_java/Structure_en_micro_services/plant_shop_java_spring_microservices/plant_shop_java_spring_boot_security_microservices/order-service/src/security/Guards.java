package security;

import model.UserDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Guards local pour order-service
 * Lit les headers X-User-Id et X-User-Admin propagés par la gateway
 */
@Component
public class Guards {

    public UserDTO requireUser() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }

        HttpServletRequest request = attrs.getRequest();
        String userIdHeader = request.getHeader("X-User-Id");
        String adminHeader = request.getHeader("X-User-Admin");

        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
        }

        try {
            int userId = Integer.parseInt(userIdHeader);
            boolean isAdmin = "true".equalsIgnoreCase(adminHeader);
            return new UserDTO(userId, isAdmin);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id invalide");
        }
    }

    public UserDTO requireAdmin() {
        UserDTO user = requireUser();
        if (!user.isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès administrateur requis");
        }
        return user;
    }
}
