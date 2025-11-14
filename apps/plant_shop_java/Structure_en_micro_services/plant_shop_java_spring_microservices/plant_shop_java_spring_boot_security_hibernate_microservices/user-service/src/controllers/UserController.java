package controllers;

import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import security.Guards;
import util.ApiMapper;
import util.PasswordUtil;
import repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    UserRepository repo;
    @Autowired
    Guards guards;

    // Spring gère plusieurs routes vers la même méthode
    @GetMapping({"/admin/users", "/users"})
    public ResponseEntity<List<?>> listUsers() throws Exception {
        guards.requireAdmin(); // Seul un admin peut lister

        List<?> payload = repo.findAll().stream()
            .sorted(userComparator())
            .map(ApiMapper::toUser)
            .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    @PatchMapping({"/admin/users/{id}", "/users/{id}"})
    public ResponseEntity<Object> updateUser(@PathVariable("id") int id, @RequestBody Map<String, Object> body) throws Exception {
        User currentUser = guards.requireUser();

        // Un utilisateur ne peut modifier que lui-même, un admin peut modifier tout le monde
        if (currentUser.id != id && !currentUser.isAdmin) {
            // Le Guard a déjà vérifié que l'utilisateur est connecté (401)
            // Ici, c'est un problème de droits (403)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User existing = repo.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (body.containsKey("name")) {
            existing.name = (String) body.get("name");
        }
        if (body.containsKey("email")) {
            existing.email = (String) body.get("email");
        }

        // **Logique critique : Seul un admin peut changer le statut admin**
        // Si un user normal (currentUser.isAdmin == false) patche son profil
        // avec "admin: true", cette condition sera fausse et le statut ignoré.
        if (currentUser.isAdmin && body.containsKey("admin")) {
            Object adminValue = body.get("admin");
            existing.isAdmin = adminValue instanceof Boolean
                ? (Boolean) adminValue
                : Boolean.parseBoolean(String.valueOf(adminValue));
        }

        if (body.containsKey("password")) {
            Object pwd = body.get("password");
            if (pwd instanceof String password && !password.isBlank()) {
                existing.passwordHash = PasswordUtil.hashPassword(password);
            }
        }

        repo.save(existing);
        return ResponseEntity.ok(ApiMapper.toUser(existing));
    }

    @DeleteMapping({"/admin/users/{id}", "/users/{id}"})
    public ResponseEntity<Void> destroyUser(@PathVariable("id") int id) throws Exception {
        guards.requireAdmin(); // Seul un admin peut supprimer
        repo.deleteById(id);
        return ResponseEntity.ok().build(); // 200 OK
    }

    // --- IMPLÉMENTATION SPÉCIFIQUE ---

    @GetMapping("/users/{id}")
    public ResponseEntity<Object> show(@PathVariable("id") int id) throws Exception {
        User currentUser = guards.requireUser();

        // Un utilisateur ne peut voir que son profil, un admin peut voir tout le monde
        if (currentUser.id != id && !currentUser.isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = repo.findById(id).orElse(null);
        return user != null
            ? ResponseEntity.ok(ApiMapper.toUser(user))
            : ResponseEntity.notFound().build();
    }

    @PostMapping("/users")
    public ResponseEntity<Object> create(@RequestBody Map<String, Object> body) throws Exception {
        guards.requireAdmin(); // Seul un admin peut créer (selon test E2E)

        String email = (String) body.get("email");
        String name = (String) body.get("name");
        String password = body.get("password") instanceof String ? (String) body.get("password") : null;
        boolean adminFlag = body.containsKey("admin")
            && Boolean.parseBoolean(String.valueOf(body.get("admin")));

        if (email == null || password == null || password.isBlank()) {
            return ResponseEntity.badRequest()
                           .body(Map.of("error", "email et password sont requis"));
        }

        if (repo.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User userData = new User(name, email, PasswordUtil.hashPassword(password), adminFlag);
        User created = repo.save(userData);

        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toUser(created));
    }

    // Helper de tri
    private Comparator<User> userComparator() {
        return Comparator.comparing((User u) -> !u.isAdmin) // Admins en premier
            .thenComparing(u -> u.name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}
