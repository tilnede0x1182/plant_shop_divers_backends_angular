package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.util.Map;
import controller.AuthController;
import model.User;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import repository.UserRepository;

/**
 * Filtre d'authentification par session.
 */
@Singleton
@Filter("/**")
public class SessionAuthFilter implements HttpServerFilter {

    private static final String SESSION_COOKIE = "session_id";
    private final UserRepository userRepo;
    private final CorsConfig cors;

    /**
 * Constructeur avec injection de dépendances.
 * @param db Connexion à la base de données
 * @param cors Configuration CORS
 */
public SessionAuthFilter(Connection db, CorsConfig cors) {
        this.userRepo = new UserRepository(db);
        this.cors = cors;
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String origin = request.getHeaders().get("Origin");
        if (cors.isAllowed(origin) && cors.isPreflight(request)) {
            return Mono.just(cors.preflight(request, origin));
        }
        return authenticate(request)
            .flatMap(req -> Mono.from(chain.proceed(req)))
            .map(response -> cors.apply(response, origin, request));
    }

    /**
 * Authentifie la requête.
 * @param request Requête HTTP
 * @return Mono avec la requête
 */
private Mono<HttpRequest<?>> authenticate(HttpRequest<?> request) {
        return Mono.fromCallable(() -> {
            request.getCookies()
                .findCookie(SESSION_COOKIE)
                .ifPresent(cookie -> attachUser(request, cookie.getValue()));
            return request;
        });
    }

    /**
 * Attache l'utilisateur à la requête.
 * @param request Requête HTTP
 * @param sessionId ID de session
 */
private void attachUser(HttpRequest<?> request, String sessionId) {
        Map<String, Integer> sessions = AuthController.getSessions();
        Integer userId = sessions.get(sessionId);
        if (userId == null) return;
        try {
            User user = userRepo.find(userId);
            if (user != null) {
                request.setAttribute("user", user);
            }
        } catch (Exception e) {
            System.err.println("Erreur DB dans le filtre: " + e.getMessage());
        }
    }
}
