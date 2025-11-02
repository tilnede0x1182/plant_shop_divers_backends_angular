package security;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import models.User;
import repositories.UserRepository;

/**
 * Filtre JAX-RS pour l'authentification par session.
 * S'exécute après le filtre CORS (par défaut).
 * @Provider l'enregistre automatiquement.
 */
@Provider
public class SessionAuthFilter implements ContainerRequestFilter {

    private static final String SESSION_COOKIE = "session_id";

    @Inject
    SessionService sessionService;

    @Inject
    AuthenticatedUser authenticatedUser;

    // On injecte un "Instance<UserRepository>" car le filtre est un Singleton
    // mais le UserRepository est @RequestScoped.
    // "Instance.get()" nous donne l'instance correcte pour cette requête.
    @Inject
    Instance<UserRepository> userRepoProvider;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        authenticatedUser.user = null;

        Cookie sessionCookie = ctx.getCookies().get(SESSION_COOKIE);
        if (sessionCookie == null) {
            return; // Pas de cookie, l'utilisateur n'est pas connecté
        }

        String sessionId = sessionCookie.getValue();
        Integer userId = sessionService.getSessions().get(sessionId);
        if (userId == null) {
            return; // Session inconnue ou expirée
        }

        try {
            User user = userRepoProvider.get().find(userId);
            if (user != null) {
                authenticatedUser.user = user;
            }
        } catch (Exception e) {
            System.err.println("Erreur DB dans le filtre d'authentification: " + e.getMessage());
        }
    }
}
