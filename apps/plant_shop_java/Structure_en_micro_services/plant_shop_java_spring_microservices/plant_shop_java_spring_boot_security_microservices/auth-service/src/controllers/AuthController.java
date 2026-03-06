package controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import repository.UserRepository;
import security.Guards;
import security.SessionService;
import util.ApiMapper;
import util.PasswordUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrôleur REST pour l'authentification.
 * Gère les endpoints de register, login, logout et récupération de l'utilisateur courant.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    UserRepository userRepo;
    @Autowired
    SessionService sessionService;
    @Autowired
    Guards guards;

    /**
     * Inscrit un nouvel utilisateur.
     * @param body Données de l'utilisateur à créer
     * @return L'utilisateur créé ou une erreur si l'email existe déjà
     * @throws Exception En cas d'erreur SQL
     */
    @PostMapping("/register")
    public ResponseEntity<Object> register(@RequestBody User body) throws Exception {
        if (userRepo.findByEmailWithPassword(body.email) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                                 .body(Map.of("error", "Cet email est déjà utilisé."));
        }

        User newUser = new User(body.name, body.email, PasswordUtil.hashPassword(body.password), false);
        int newId = userRepo.create(newUser);
        User created = userRepo.find(newId);

        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toUser(created));
    }

    /**
     * Authentifie un utilisateur et crée une session.
     * @param body Map contenant email et password
     * @param response Réponse HTTP pour définir le cookie de session
     * @return L'utilisateur authentifié ou une erreur
     * @throws Exception En cas d'erreur SQL
     */
    @PostMapping("/login")
    public ResponseEntity<Object> login(@RequestBody Map<String, String> body, HttpServletResponse response) throws Exception {
        User user = userRepo.findByEmailWithPassword(body.get("email"));

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

        // Ajouter SameSite via header Set-Cookie
        String cookieValue = String.format("%s=%s; Path=%s; Max-Age=%d; HttpOnly; SameSite=Lax",
            "session_id", sessionId, "/", 3600);
        response.setHeader("Set-Cookie", cookieValue);
        authenticateUser(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toUser(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "session_id", required = false) String sessionId, HttpServletResponse response) {
        if (sessionId != null) {
            sessionService.removeSession(sessionId);
        }

        // Crée un cookie expiré pour l'effacer avec SameSite
        String expiredCookieValue = String.format("%s=; Path=%s; Max-Age=%d; HttpOnly; SameSite=Lax",
            "session_id", "/", 0);
        response.setHeader("Set-Cookie", expiredCookieValue);

        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build(); // 204 No Content
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me(@CookieValue(name = "session_id", required = false) String sessionId) throws Exception {
        User user = resolveUserFromSession(sessionId);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body(Map.of("error", "Authentification requise"));
        }
        return ResponseEntity.ok(ApiMapper.toUser(user));
    }

    @GetMapping("/_session")
    public ResponseEntity<Object> session(@CookieValue(name = "session_id", required = false) String sessionId) throws Exception {
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

    /**
     * Configure l'authentification Spring Security pour l'utilisateur.
     * @param user Utilisateur à authentifier dans le contexte de sécurité
     */
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

    /**
     * Résout un utilisateur à partir de son identifiant de session.
     * @param sessionId Identifiant de session
     * @return L'utilisateur correspondant ou null si la session est invalide
     * @throws Exception En cas d'erreur SQL
     */
    private User resolveUserFromSession(String sessionId) throws Exception {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        Integer userId = sessionService.getSession(sessionId);
        if (userId == null) {
            return null;
        }
        return userRepo.find(userId);
    }
}
