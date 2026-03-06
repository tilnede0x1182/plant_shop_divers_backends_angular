package controllers;

import models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repositories.UserRepository;
import security.Guards;
import utils.ApiMapper;
import utils.PasswordUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour les utilisateurs.
 */
@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    UserRepository repo;
    @Autowired
    Guards guards;

    /**
     * Liste tous les utilisateurs (admin requis).
     * @return ResponseEntity Liste des utilisateurs
     * @throws Exception En cas d'erreur
     */
    @GetMapping({"/admin/users", "/users"})
    public ResponseEntity<List<?>> listUsers() throws Exception {
        guards.requireAdmin(); // Seul un admin peut lister

        List<?> payload = repo.list().stream()
            .sorted(userComparator())
            .map(ApiMapper::toUser)
            .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    /**
     * Met à jour un utilisateur.
     * @param id int ID de l'utilisateur
     * @param body Map Corps de la requête
     * @return ResponseEntity Utilisateur mis à jour
     * @throws Exception En cas d'erreur
     */
    @PatchMapping({"/admin/users/{id}", "/users/{id}"})
    public ResponseEntity<Object> updateUser(@PathVariable("id") int id, @RequestBody Map<String, Object> body) throws Exception {
        User currentUser = guards.requireUser();

        // Un utilisateur ne peut modifier que lui-même, un admin peut modifier tout le monde
        if (currentUser.id != id && !currentUser.isAdmin) {
            // Le Guard a déjà vérifié que l'utilisateur est connecté (401)
            // Ici, c'est un problème de droits (403)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User existing = repo.find(id);
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

        repo.update(existing);
        return ResponseEntity.ok(ApiMapper.toUser(repo.find(id)));
    }

    /**
     * Supprime un utilisateur (admin requis).
     * @param id int ID de l'utilisateur
     * @return ResponseEntity Réponse vide
     * @throws Exception En cas d'erreur
     */
    @DeleteMapping({"/admin/users/{id}", "/users/{id}"})
    public ResponseEntity<Void> destroyUser(@PathVariable("id") int id) throws Exception {
        guards.requireAdmin(); // Seul un admin peut supprimer
        repo.delete(id);
        return ResponseEntity.ok().build(); // 200 OK
    }

    /**
     * Affiche un utilisateur par son ID.
     * @param id int ID de l'utilisateur
     * @return ResponseEntity Utilisateur
     * @throws Exception En cas d'erreur
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<Object> show(@PathVariable("id") int id) throws Exception {
        User currentUser = guards.requireUser();

        // Un utilisateur ne peut voir que son profil, un admin peut voir tout le monde
        if (currentUser.id != id && !currentUser.isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        User user = repo.find(id);
        return user != null
            ? ResponseEntity.ok(ApiMapper.toUser(user))
            : ResponseEntity.notFound().build();
    }

    /**
     * Crée un nouvel utilisateur (admin requis).
     * @param body Map Corps de la requête
     * @return ResponseEntity Utilisateur créé
     * @throws Exception En cas d'erreur
     */
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

        if (repo.findByEmailWithPassword(email) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        User userData = new User(name, email, PasswordUtil.hashPassword(password), adminFlag);
        int newId = repo.create(userData);

        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toUser(repo.find(newId)));
    }

    /**
     * Comparateur pour trier les utilisateurs.
     * @return Comparator Comparateur
     */
    private Comparator<User> userComparator() {
        return Comparator.comparing((User u) -> !u.isAdmin) // Admins en premier
            .thenComparing(u -> u.name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}
