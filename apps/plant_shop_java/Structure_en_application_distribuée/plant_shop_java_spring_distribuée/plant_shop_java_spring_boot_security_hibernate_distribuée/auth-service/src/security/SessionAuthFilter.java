package security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import repository.UserRepository;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // S'exécute juste après CorsFilter
public class SessionAuthFilter extends OncePerRequestFilter {

    private static final String SESSION_COOKIE = "session_id";

    @Autowired
    SessionService sessionService;

    @Autowired
    AuthenticatedUser authenticatedUser;

    @Autowired
    UserRepository userRepo; // Le repo est @RequestScope, Spring injecte le bon proxy

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        SecurityContextHolder.clearContext();
        authenticatedUser.setUser(null); // Réinitialise l'utilisateur pour cette requête

        Cookie sessionCookie = extractSessionCookie(request);
        if (sessionCookie != null) {
            handleSession(sessionCookie.getValue());
        }

        filterChain.doFilter(request, response);
    }

    private Cookie extractSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (SESSION_COOKIE.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    private void handleSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        Integer userId = sessionService.getSessions().get(sessionId);
        if (userId == null) {
            System.out.println("⚠️ Session inconnue pour id=" + sessionId);
            return; // Session inconnue ou expirée
        }

        try {
            User user = userRepo.findById(userId).orElse(null);
            if (user != null) {
                authenticatedUser.setUser(user); // Stocke l'utilisateur pour la requête
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        buildAuthorities(user.isAdmin)
                    );
                authentication.setDetails(user);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            System.err.println("Erreur DB dans le filtre d'authentification: " + e.getMessage());
            // Continue quand même, l'utilisateur sera juste "non connecté"
        }
    }

    private List<SimpleGrantedAuthority> buildAuthorities(boolean isAdmin) {
        if (isAdmin) {
            return List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
