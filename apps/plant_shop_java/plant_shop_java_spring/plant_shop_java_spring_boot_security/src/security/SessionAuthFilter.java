package security;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import repositories.UserRepository;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // S'exécute juste après CorsFilter
public class SessionAuthFilter implements Filter {

    private static final String SESSION_COOKIE = "session_id";

    @Autowired
    SessionService sessionService;

    @Autowired
    AuthenticatedUser authenticatedUser;

    @Autowired
    UserRepository userRepo; // Le repo est @RequestScope, Spring injecte le bon proxy

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        authenticatedUser.setUser(null); // Réinitialise l'utilisateur pour cette requête

        if (request.getCookies() == null) {
            chain.doFilter(req, res);
            return;
        }

        Cookie sessionCookie = null;
        for (Cookie cookie : request.getCookies()) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                sessionCookie = cookie;
                break;
            }
        }

        if (sessionCookie == null) {
            chain.doFilter(req, res);
            return; // Pas de cookie, l'utilisateur n'est pas connecté
        }

        String sessionId = sessionCookie.getValue();
        Integer userId = sessionService.getSessions().get(sessionId);

        if (userId == null) {
            chain.doFilter(req, res);
            return; // Session inconnue ou expirée
        }

        try {
            User user = userRepo.find(userId);
            if (user != null) {
                authenticatedUser.setUser(user); // Stocke l'utilisateur pour la requête
            }
        } catch (Exception e) {
            System.err.println("Erreur DB dans le filtre d'authentification: " + e.getMessage());
            // Continue quand même, l'utilisateur sera juste "non connecté"
        }

        chain.doFilter(req, res);
    }
}
