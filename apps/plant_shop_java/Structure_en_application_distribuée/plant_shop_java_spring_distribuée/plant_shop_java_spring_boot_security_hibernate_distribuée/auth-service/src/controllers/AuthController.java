package controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import security.Guards;
import security.SessionService;
import utils.ApiMapper;
import utils.PasswordUtil;
import repositories.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
        String sessionId = UUID.randomUUID().toString();
        sessionService.getSessions().put(sessionId, user.id);

        Cookie cookie = new Cookie("session_id", sessionId);
        cookie.setPath("/");
        cookie.setMaxAge(3600); // 1 heure
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // À activer en production (HTTPS)
        response.addCookie(cookie);
        authenticateUser(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toUser(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "session_id", required = false) String sessionId, HttpServletResponse response) {
        if (sessionId != null) {
            sessionService.removeSession(sessionId);
        }

        // Crée un cookie expiré pour l'effacer
        Cookie expiredCookie = new Cookie("session_id", "");
        expiredCookie.setPath("/");
        expiredCookie.setMaxAge(0); // Expire immédiatement
        expiredCookie.setHttpOnly(true);
        response.addCookie(expiredCookie);

        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me() {
        // Le Guard lève une AuthException (401) si l'utilisateur n'est pas trouvé
        User user = guards.requireUser();
        return ResponseEntity.ok(ApiMapper.toUser(user));
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
}
