package security;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Singleton;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;
import model.User;
import org.reactivestreams.Publisher;
import controller.AuthController;
import repository.UserRepository;
import reactor.core.publisher.Mono;

@Singleton
@Filter("/**")
public class SessionAuthFilter implements HttpServerFilter {

    private final UserRepository userRepo;
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
        "http://localhost:8300",
        "http://127.0.0.1:8300"
    );

    public SessionAuthFilter(Connection db) {
        this.userRepo = new UserRepository(db);
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        // CORS Headers
        String origin = request.getHeaders().get("Origin");
        if (isAllowedOrigin(origin)) {
            if ("OPTIONS".equals(request.getMethodName())) {
                MutableHttpResponse<?> response = io.micronaut.http.HttpResponse.ok();
                configureCorsHeaders(response, origin, request);
                return Mono.just(response);
            }
        }

        // Session Authentication
        return Mono.fromCallable(() -> {
            request.getCookies().findCookie("session_id").ifPresent(cookie -> {
                Map<String, Integer> sessions = AuthController.getSessions();
                Integer userId = sessions.get(cookie.getValue());
                if (userId != null) {
                    try {
                        User user = userRepo.find(userId);
                        if (user != null) {
                            request.setAttribute("user", user);
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur DB dans le filtre: " + e.getMessage());
                    }
                }
            });
            return request;
        }).flatMap(req -> Mono.from(chain.proceed(req)))
          .map(response -> {
              if (isAllowedOrigin(origin)) {
                  configureCorsHeaders(response, origin, request);
              }
              return response;
          });
    }

    private boolean isAllowedOrigin(String origin) {
        return origin != null && ALLOWED_ORIGINS.contains(origin);
    }

    private void configureCorsHeaders(MutableHttpResponse<?> response, String origin, HttpRequest<?> request) {
        response.header("Access-Control-Allow-Origin", origin);
        response.header("Access-Control-Allow-Credentials", "true");
        response.header("Vary", "Origin");
        if ("OPTIONS".equals(request.getMethodName())) {
            response.header("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE, OPTIONS");
            String reqHeaders = request.getHeaders().get("Access-Control-Request-Headers");
            response.header("Access-Control-Allow-Headers", reqHeaders != null ? reqHeaders : "Content-Type, Cookie");
        }
    }
}
