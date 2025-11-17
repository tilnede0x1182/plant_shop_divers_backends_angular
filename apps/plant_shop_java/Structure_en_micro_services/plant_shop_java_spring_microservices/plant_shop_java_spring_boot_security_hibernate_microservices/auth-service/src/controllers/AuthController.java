package controllers;

import jakarta.servlet.http.HttpServletResponse;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import security.Guards;
import security.SessionService;
import util.ApiMapper;
import util.PasswordUtil;
import repository.UserRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final int SESSION_TTL_SECONDS = 3600;

    @Autowired
    UserRepository userRepo;
    @Autowired
    SessionService sessionService;
    @Autowired
    Guards guards;

    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody User body) throws Exception {
        if (userRepo.existsByEmail(body.email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(Map.of("error", "Cet email est déjà utilisé."));
        }

        User newUser = new User(body.name, body.email, PasswordUtil.hashPassword(body.password), false);
        User created = userRepo.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toUser(created));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> body, HttpServletResponse response) throws Exception {
        User user = userRepo.findByEmail(body.get("email")).orElse(null);

        if (user == null || !PasswordUtil.checkPassword(body.get("password"), user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("error", "Identifiants invalides"));
        }
        String sessionId = sessionService.createSession(user);
        setSessionCookie(response, sessionId, SESSION_TTL_SECONDS);
        authenticateUser(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toUser(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "session_id", required = false) String sessionId, HttpServletResponse response) {
        if (sessionId != null) {
            sessionService.removeSession(sessionId);
        }

        setSessionCookie(response, "", 0);

        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me() {
        // Le Guard lève une AuthException (401) si l'utilisateur n'est pas trouvé
        User user = guards.requireUser();
        return ResponseEntity.ok(ApiMapper.toUser(user));
    }

    @GetMapping("/_session")
    public ResponseEntity<Object> session(@CookieValue(name = "session_id", required = false) String sessionId) {
        User user = resolveUserFromSession(sessionId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("error", "Authentification requise"));
        }
        return ResponseEntity.ok(Map.of(
            "id", user.id,
            "admin", user.isAdmin
        ));
    }

    private void authenticateUser(User user) {
        List<SimpleGrantedAuthority> authorities = user.isAdmin
            ? List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("ROLE_USER")
            )
            : List.of(new SimpleGrantedAuthority("ROLE_USER"));

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user, null, authorities);
        authentication.setDetails(user);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User resolveUserFromSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return sessionService.getSession(sessionId);
    }

    private void setSessionCookie(HttpServletResponse response, String value, int maxAgeSeconds) {
        String cookieValue = String.format(
            "session_id=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
            value,
            Math.max(0, maxAgeSeconds)
        );
        response.setHeader("Set-Cookie", cookieValue);
    }
}
