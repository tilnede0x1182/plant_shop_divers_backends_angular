package controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.UUID;
import models.User;
import repositories.UserRepository;
import security.Guards;
import security.SessionService;
import util.ApiMapper;
import util.PasswordUtil;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
/**
 * Contrôleur REST pour l'authentification.
 * Gère l'inscription, la connexion, la déconnexion et la récupération du profil.
 */
@RequestScoped // Par défaut dans Quarkus, mais explicite c'est bien
public class AuthController {

    @Inject
    UserRepository userRepo;
    @Inject
    SessionService sessionService;

    @Inject
    Guards guards;

    /**
     * Inscrit un nouvel utilisateur.
     *
     * @param body Corps de la requête contenant name, email et password
     * @return Réponse HTTP avec l'utilisateur créé ou une erreur
     * @throws Exception En cas d'erreur lors de la création
     */
    @POST
    @Path("/register")
    public Response register(Map<String, String> body) throws Exception {
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");

        if (userRepo.findByEmailWithPassword(email) != null) {
            return Response.status(Response.Status.CONFLICT)
                           .entity(Map.of("error", "Cet email est déjà utilisé."))
                           .build();
        }

        User newUser = new User(name, email, PasswordUtil.hashPassword(password), false);
        int newId = userRepo.create(newUser);
        User created = userRepo.find(newId);

        return Response.status(Response.Status.CREATED)
                       .entity(ApiMapper.toUser(created))
                       .build();
    }

    /**
     * Authentifie un utilisateur et crée une session.
     *
     * @param body Corps de la requête contenant email et password
     * @return Réponse HTTP avec l'utilisateur et un cookie de session
     * @throws Exception En cas d'erreur lors de l'authentification
     */
    @POST
    @Path("/login")
    public Response login(Map<String, String> body) throws Exception {
        User user = userRepo.findByEmailWithPassword(body.get("email"));
        if (user == null || !PasswordUtil.checkPassword(body.get("password"), user.passwordHash)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(Map.of("error", "Identifiants invalides"))
                           .build();
        }

        String sessionId = UUID.randomUUID().toString();
        sessionService.getSessions().put(sessionId, user.id);
        NewCookie cookie = new NewCookie.Builder("session_id")
            .value(sessionId)
            .path("/")
            .maxAge(3600) // 1 heure
            .httpOnly(true)
            .build();
        return Response.status(Response.Status.CREATED)
                       .cookie(cookie)
                       .entity(ApiMapper.toUser(user))
                       .build();
    }

    /**
     * Déconnecte l'utilisateur en supprimant sa session.
     *
     * @param sessionId Identifiant de session depuis le cookie
     * @return Réponse HTTP 204 No Content
     */
    @POST
    @Path("/logout")
    public Response logout(@CookieParam("session_id") String sessionId) {
        if (sessionId != null) {
            sessionService.getSessions().remove(sessionId);
        }

        // Crée un cookie expiré pour l'effacer
        NewCookie expiredCookie = new NewCookie.Builder("session_id")
            .value("")
            .path("/")
            .maxAge(0) // Expire immédiatement
            .httpOnly(true)
            .build();
        // 204 No Content
        return Response.noContent().cookie(expiredCookie).build();
    }

    /**
     * Récupère le profil de l'utilisateur authentifié.
     *
     * @return Réponse HTTP avec les informations de l'utilisateur
     */
    @GET
    @Path("/me")
    public Response me() {
        // Le Guard lève une 401 si l'utilisateur n'est pas trouvé
        // Le Guards utilise maintenant le ForwardedIdentity pour vérifier l'authentification
        User user = guards.requireUser();
        return Response.ok(ApiMapper.toUser(user)).build();
    }

    /**
     * Endpoint interne pour valider une session et récupérer l'identité.
     * Utilisé par la Gateway pour propager l'authentification.
     *
     * @param sessionId Identifiant de session depuis le cookie
     * @return Réponse HTTP avec l'ID et le statut admin de l'utilisateur
     * @throws Exception En cas d'erreur lors de la récupération
     */
    @GET
    @Path("/_session")
    public Response session(@CookieParam("session_id") String sessionId) throws Exception {
        if (sessionId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(Map.of("error", "Authentification requise"))
                           .build();
        }

        Integer userId = sessionService.getSessions().get(sessionId);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(Map.of("error", "Session invalide"))
                           .build();
        }

        User user = userRepo.find(userId);
        if (user == null) {
            sessionService.getSessions().remove(sessionId);
            return Response.status(Response.Status.UNAUTHORIZED)
                           .entity(Map.of("error", "Session expirée"))
                           .build();
        }

        return Response.ok(Map.of(
            "id", user.id,
            "admin", user.isAdmin
        )).build();
    }
}
