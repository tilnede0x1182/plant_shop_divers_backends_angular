// src/controllers/UserController.java
package controller;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import model.User;
import org.json.JSONObject;
import repository.UserRepository;
import util.ApiMapper;
import util.PasswordUtil;

/**
 * Contrôleur pour les utilisateurs.
 * Gère les opérations CRUD sur les utilisateurs.
 */
public final class UserController {

    private final UserRepository repo;

    /**
     * Constructeur.
     * @param db Connection Connexion DB
     */
    public UserController(Connection db) {
        this.repo = new UserRepository(db);
    }

    /**
     * Liste tous les utilisateurs.
     * @param ctx Context Contexte Javalin
     * @throws Exception En cas d erreur
     */
    public void list(Context ctx) throws Exception {
        List<User> users = repo.list();
        users.sort(userComparator());
        ctx.json(mapUsers(users));
    }

    /**
     * Affiche un utilisateur.
     * @param ctx Context Contexte Javalin
     * @throws Exception En cas d erreur
     */
    public void show(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        User currentUser = ctx.attribute("user");
        if (currentUser.id != id && !currentUser.isAdmin) {
            throw new ForbiddenResponse();
        }
        User user = repo.find(id);
        if (user == null) throw new NotFoundResponse();
        ctx.json(ApiMapper.toUser(user));
    }

    /**
     * Crée un utilisateur.
     * @param ctx Context Contexte Javalin
     * @throws Exception En cas d erreur
     */
    public void create(Context ctx) throws Exception {
        JSONObject body = new JSONObject(ctx.body());
        if (!body.has("email") || !body.has("name") || !body.has("password")) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Champs email, name et password requis"));
            return;
        }
        String name = body.getString("name");
        String email = body.getString("email");
        String password = body.getString("password");
        boolean isAdmin = body.optBoolean("admin", false);

        if (repo.findByEmailWithPassword(email) != null) {
            ctx.status(HttpStatus.CONFLICT).json(Map.of("error", "Cet email est déjà utilisé"));
            return;
        }

        User newUser = new User(name, email, PasswordUtil.hashPassword(password), isAdmin);
        int newId = repo.create(newUser);
        User created = repo.find(newId);
        ctx.status(HttpStatus.CREATED).json(ApiMapper.toUser(created));
    }

    /**
     * Met à jour un utilisateur.
     * @param ctx Context Contexte Javalin
     * @throws Exception En cas d erreur
     */
    public void update(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        User currentUser = ctx.attribute("user");
        if (currentUser.id != id && !currentUser.isAdmin) {
            throw new ForbiddenResponse();
        }
        User userToUpdate = repo.find(id);
        if (userToUpdate == null) throw new NotFoundResponse();

        JSONObject body = new JSONObject(ctx.body());
        if (body.has("name") && !body.isNull("name")) {
            userToUpdate.name = body.getString("name");
        }
        if (body.has("email") && !body.isNull("email")) {
            String newEmail = body.getString("email");
            User existing = repo.findByEmailWithPassword(newEmail);
            if (existing != null && existing.id != id) {
                ctx.status(HttpStatus.CONFLICT).json(Map.of("error", "Cet email est déjà utilisé"));
                return;
            }
            userToUpdate.email = newEmail;
        }
        if (body.has("admin") && currentUser.isAdmin) {
            userToUpdate.isAdmin = body.getBoolean("admin");
        }
        if (body.has("password") && currentUser.id == id) {
            String password = body.getString("password");
            userToUpdate.passwordHash = PasswordUtil.hashPassword(password);
        }
        repo.update(userToUpdate);
        ctx.json(ApiMapper.toUser(repo.find(id)));
    }

    /**
     * Supprime un utilisateur.
     * @param ctx Context Contexte Javalin
     * @throws Exception En cas d erreur
     */
    public void destroy(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        repo.delete(id);
        ctx.status(HttpStatus.OK).json(Map.of("deleted", true));
    }

    /**
     * Convertit une liste d utilisateurs en liste de maps.
     * @param users List Liste d utilisateurs
     * @return List Liste de maps
     */
    private List<Map<String, Object>> mapUsers(List<User> users) {
        List<Map<String, Object>> mapped = new ArrayList<>(users.size());
        for (User user : users) {
            mapped.add(ApiMapper.toUser(user));
        }
        return mapped;
    }

    /**
     * Retourne un comparateur pour trier les utilisateurs.
     * @return Comparator Comparateur admins puis alphabétique
     */
    private Comparator<User> userComparator() {
        return (a, b) -> {
            if (a.isAdmin != b.isAdmin) {
                return a.isAdmin ? -1 : 1;
            }
            String nameA = a.name == null ? "" : a.name.toLowerCase();
            String nameB = b.name == null ? "" : b.name.toLowerCase();
            return nameA.compareTo(nameB);
        };
    }
}
