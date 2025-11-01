package controller;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.Connection;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import model.User;
import repository.UserRepository;
import util.PasswordUtil;
import util.Request;
import util.Response;
import org.json.JSONObject;

public final class AuthController extends BaseController {

    private final UserRepository userRepo;
    // AJOUTÉ: Rendre la map de sessions statique et publique pour être accessible par BaseController
    private static final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    public AuthController(Connection db) {
        super(db); // AJOUTÉ: Appel au constructeur parent
        this.userRepo = new UserRepository(db);
    }

    // AJOUTÉ: Getter pour que BaseController puisse accéder aux sessions
    public static Map<String, Integer> getSessions() {
        return sessions;
    }

		@Override
		public void handle(HttpExchange ex) throws IOException {
			String path = ex.getRequestURI().getPath().substring("/api/auth".length());

			try {
				if ("POST".equals(ex.getRequestMethod())) {
					if ("/register".equals(path)) {
						register(ex);
						return;
					}
					if ("/login".equals(path)) {
						login(ex);
						return;
					}
				}
				if ("GET".equals(ex.getRequestMethod())) {
					if ("/me".equals(path)) {
						me(ex);
						return;
					}
				}
				// Accepte POST, GET, DELETE pour /logout (plus de 404)
				if ("/logout".equals(path)) {
					logout(ex);
					return;
				}
				sendJsonResponse(ex, 404, "{\"error\":\"Route non trouvée dans AuthController\"}");
			} catch (Exception e) {
				handleError(ex, e);
			}
		}

    private void register(HttpExchange ex) throws Exception {
        JSONObject body = parseJsonBody(ex);
        String name = body.optString("name", null);
        String email = body.optString("email", null);
        String password = body.optString("password", null);

        if (name == null || email == null || password == null) {
            sendJsonResponse(ex, 400, "{\"error\":\"Les champs 'name', 'email' et 'password' sont requis.\"}");
            return;
        }

        if (userRepo.findByEmailWithPassword(email) != null) {
            sendJsonResponse(ex, 409, "{\"error\":\"Cet email est déjà utilisé.\"}");
            return;
        }

        String passwordHash = PasswordUtil.hashPassword(password);
        User newUser = new User(name, email, passwordHash, false);

        int userId = userRepo.create(newUser);

        // CORRIGÉ: Le test attend un corps JSON vide pour un code 201, mais renvoyer un message est plus informatif.
        // Le test `Test.java` gère un corps vide ou un objet JSON, donc c'est compatible.
        sendJsonResponse(ex, 201, "{\"message\":\"Utilisateur créé avec succès.\", \"userId\":" + userId + "}");
    }

    private void login(HttpExchange ex) throws Exception {
        JSONObject body = parseJsonBody(ex);
        String email = body.optString("email", null);
        String password = body.optString("password", null);

        if (email == null || password == null) {
            sendJsonResponse(ex, 400, "{\"error\":\"Les champs 'email' et 'password' sont requis.\"}");
            return;
        }

        User user = userRepo.findByEmailWithPassword(email);
        if (user == null || !PasswordUtil.checkPassword(password, user.passwordHash)) {
            sendJsonResponse(ex, 401, "{\"error\":\"Email ou mot de passe incorrect.\"}");
            return;
        }

        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user.id);

				ex.getResponseHeaders().add("Set-Cookie",
						"session_id=" + sessionId + "; HttpOnly; Path=/api; Max-Age=3600; SameSite=Lax");
        // Le test attend un corps vide ou JSON, donc c'est ok.
        sendJsonResponse(ex, 201, "{\"message\":\"Connexion réussie.\"}");
    }

		private void logout(HttpExchange ex) throws Exception {
			String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
			if (cookieHeader != null) {
				java.util.Arrays.stream(cookieHeader.split(";"))
						.map(String::trim)
						.filter(c -> c.startsWith("session_id="))
						.map(c -> c.substring("session_id=".length()))
						.findFirst()
						.ifPresent(sessions::remove);
			}
			// Cookie expiré -> effacement côté navigateur
			ex.getResponseHeaders().add(
					"Set-Cookie",
					"session_id=deleted; Path=/api; Max-Age=0; HttpOnly; SameSite=Lax"
			);
			sendEmptyResponse(ex, 204);   // helper déjà présent dans BaseController :contentReference[oaicite:0]{index=0}
		}

		private void me(HttpExchange ex) throws Exception {
			User currentUser = getAuthenticatedUser(ex);

			if (currentUser == null) {
				sendJsonResponse(ex, 401, "{\"error\":\"Non authentifié.\"}");
				return;
			}

			JSONObject userJson = new JSONObject();
			userJson.put("id", currentUser.id);
			userJson.put("name", currentUser.name);
			userJson.put("email", currentUser.email);
			userJson.put("is_admin", currentUser.isAdmin);

			sendJsonResponse(ex, 200, userJson.toString());
		}
}
