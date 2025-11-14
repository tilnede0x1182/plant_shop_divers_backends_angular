package controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.cookie.Cookie;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import model.User;
import repository.UserRepository;
import util.ApiMapper;
import util.PasswordUtil;
import io.micronaut.http.HttpRequest;

@Controller("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private static final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    @Inject
    public AuthController(Connection db) {
        this.userRepo = new UserRepository(db);
    }

    public static Map<String, Integer> getSessions() {
        return sessions;
    }

    @Post("/register")
    public HttpResponse<?> register(@Body Map<String, String> body) throws Exception {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");

        if (userRepo.findByEmailWithPassword(email) != null) {
            return HttpResponse.status(HttpStatus.CONFLICT).body(Map.of("error", "Cet email est déjà utilisé."));
        }

        User newUser = new User(name, email, PasswordUtil.hashPassword(password), false);
        int newId = userRepo.create(newUser);
        User created = userRepo.find(newId);
        return HttpResponse.created(ApiMapper.toUser(created));
    }

    @Post("/login")
    public MutableHttpResponse<?> login(@Body Map<String, String> body) throws Exception {
        User user = userRepo.findByEmailWithPassword(body.get("email"));
        if (user == null || !PasswordUtil.checkPassword(body.get("password"), user.passwordHash)) {
            return HttpResponse.unauthorized().body(Map.of("error", "Identifiants invalides"));
        }

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user.id);

        Cookie cookie = Cookie.of("session_id", sessionId)
            .path("/")
            .httpOnly(true)
            .maxAge(3600);

        return HttpResponse.status(HttpStatus.CREATED)
            .cookie(cookie)
            .body(ApiMapper.toUser(user));
    }

    @Post("/logout")
    public HttpResponse<?> logout(@CookieValue("session_id") String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
        Cookie expiredCookie = Cookie.of("session_id", "").path("/").maxAge(0);
        return HttpResponse.noContent().cookie(expiredCookie);
    }

    @Get("/me")
    public HttpResponse<?> me(HttpRequest<?> request) {
        User user = request.getAttribute("user", User.class).orElse(null);
        if (user == null) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.ok(ApiMapper.toUser(user));
    }
}
