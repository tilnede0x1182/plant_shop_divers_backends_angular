package controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import models.User;
import repositories.UserRepository;
import security.Guards;
import util.ApiMapper;
import util.PasswordUtil;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class UserController {

    @Inject
    UserRepository repo;

    @Inject
    Guards guards;

    /**
     * Liste tous les utilisateurs (endpoint admin).
     *
     * @return Reponse HTTP avec la liste des utilisateurs
     * @throws Exception En cas d'erreur
     */
    @GET
    @Path("/admin/users")
    public Response listAdmin() throws Exception {
        return listImpl();
    }

    @GET
    @Path("/users")
    public Response listAlias() throws Exception {
        return listImpl();
    }

    @PATCH
    @Path("/admin/users/{id}")
    @Transactional
    public Response updateAdminAlias(@PathParam("id") int id, Map<String, Object> body) throws Exception {
        return updateImpl(id, body);
    }

    @PATCH
    @Path("/users/{id}")
    @Transactional
    public Response updateUser(@PathParam("id") int id, Map<String, Object> body) throws Exception {
        return updateImpl(id, body);
    }

    @DELETE
    @Path("/admin/users/{id}")
    @Transactional
    public Response destroyAdminAlias(@PathParam("id") int id) throws Exception {
        return destroyImpl(id);
    }

    @DELETE
    @Path("/users/{id}")
    @Transactional
    public Response destroyUser(@PathParam("id") int id) throws Exception {
        return destroyImpl(id);
    }

    // --- IMPLÉMENTATION ---

    /**
     * Implementation de la liste des utilisateurs.
     *
     * @return Reponse HTTP avec la liste
     * @throws Exception En cas d'erreur
     */
    private Response listImpl() throws Exception {
        guards.requireAdmin();
        // Seul un admin peut lister les utilisateurs
        List<?> payload = repo.list().stream()
            .sorted(userComparator())
            .map(ApiMapper::toUser)
            .collect(Collectors.toList());
        return Response.ok(payload).build();
    }

    /**
     * Recupere un utilisateur par son ID.
     *
     * @param id ID de l'utilisateur
     * @return Reponse HTTP avec l'utilisateur ou 404
     * @throws Exception En cas d'erreur
     */
    @GET
    @Path("/users/{id}")
    public Response show(@PathParam("id") int id) throws Exception {
        User currentUser = guards.requireUser();
        // Un utilisateur ne peut voir que son profil, un admin peut voir tout le monde
        if (currentUser.id != id && !currentUser.isAdmin) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User user = repo.find(id);
        return user != null
            ?
        Response.ok(ApiMapper.toUser(user)).build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * Cree un nouvel utilisateur.
     * Requiert un utilisateur administrateur.
     *
     * @param body Corps de la requete contenant les donnees
     * @return Reponse HTTP 201 avec l'utilisateur cree
     * @throws Exception En cas d'erreur
     */
    @POST
    @Path("/users")
    @Transactional
    public Response create(Map<String, Object> body) throws Exception {
        guards.requireAdmin();
        // Seul un admin peut créer un utilisateur (selon test)

        String email = (String) body.get("email");
        String name = (String) body.get("name");
        String password = body.get("password") instanceof String ? (String) body.get("password") : null;
        boolean adminFlag = body.containsKey("admin")
            && Boolean.parseBoolean(String.valueOf(body.get("admin")));
        if (email == null || password == null || password.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "email et password sont requis"))
                           .build();
        }

        if (repo.findByEmailWithPassword(email) != null) {
            return Response.status(Response.Status.CONFLICT).build();
        }

        User userData = new User(name, email, PasswordUtil.hashPassword(password), adminFlag);
        int newId = repo.create(userData);
        return Response.status(Response.Status.CREATED)
                       .entity(ApiMapper.toUser(repo.find(newId)))
                       .build();
    }

    /**
     * Implementation de la mise a jour d'un utilisateur.
     *
     * @param id ID de l'utilisateur
     * @param body Corps de la requete
     * @return Reponse HTTP avec l'utilisateur mis a jour
     * @throws Exception En cas d'erreur
     */
    private Response updateImpl(int id, Map<String, Object> body) throws Exception {
        User currentUser = guards.requireUser();
        // Un utilisateur ne peut modifier que lui-même, un admin peut modifier tout le monde
        if (currentUser.id != id && !currentUser.isAdmin) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        User existing = repo.find(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (body.containsKey("name")) {
            existing.name = (String) body.get("name");
        }
        if (body.containsKey("email")) {
            existing.email = (String) body.get("email");
        }

        // **Logique critique pour la sécurité (demandée par l'utilisateur)**
        // Seul un admin peut changer le statut admin de quelqu'un.
        // Si un user normal (currentUser.isAdmin == false) patche son profil
        // avec "admin: true", cette condition sera fausse et le statut ignoré.
        if (currentUser.isAdmin && body.containsKey("admin")) {
            Object adminValue = body.get("admin");
            existing.isAdmin = adminValue instanceof Boolean ? (Boolean) adminValue : Boolean.parseBoolean(String.valueOf(adminValue));
        }

        if (body.containsKey("password")) {
            Object pwd = body.get("password");
            if (pwd instanceof String password && !password.isBlank()) {
                existing.passwordHash = PasswordUtil.hashPassword(password);
            }
        }

        repo.update(existing);
        return Response.ok(ApiMapper.toUser(repo.find(id))).build();
    }

    /**
     * Implementation de la suppression d'un utilisateur.
     *
     * @param id ID de l'utilisateur
     * @return Reponse HTTP 200
     * @throws Exception En cas d'erreur
     */
    private Response destroyImpl(int id) throws Exception {
        guards.requireAdmin();
        // Seul un admin peut supprimer un utilisateur
        repo.delete(id);
        return Response.ok().build();
        // 200 OK attendu par le test
    }

    /**
     * Retourne un comparateur pour trier les utilisateurs (admins en premier, puis par nom).
     *
     * @return Comparateur d'utilisateurs
     */
    private Comparator<User> userComparator() {
        return Comparator.comparing((User u) -> !u.isAdmin) // Admins en premier
            .thenComparing(u -> u.name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }
}
