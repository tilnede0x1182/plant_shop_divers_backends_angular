import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import util.AuthContext;
import util.PasswordUtil;

import java.io.IOException;
import java.sql.Connection;
import java.util.Comparator;
import java.util.List;

/**
 * Routes HTTP pour le service utilisateur.
 */
final class UserRoutes implements HttpHandler {

    private final UserController controller;

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	public UserRoutes(Connection db) {
        this.controller = new UserController(db);
    }

    /**
     * Traite une requête HTTP.
     * @param exchange L'échange HTTP
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        controller.handle(exchange);
    }
}

/**
 * Contrôleur de base avec méthodes utilitaires.
 */
abstract class UserBaseController {
    protected final Connection db;

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	UserBaseController(Connection db) {
        this.db = db;
    }

    /**
	 * Parse le corps JSON.
	 * @param ex L'échange HTTP
	 * @return L'objet JSON
	 */
	protected JSONObject parseJson(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return body.isBlank() ? new JSONObject() : new JSONObject(body);
    }

    /**
	 * Envoie une réponse JSON Object.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 * @param body Le corps JSON
	 */
	protected void sendJson(HttpExchange ex, int code, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse JSON Array.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 * @param body Le tableau JSON
	 */
	protected void sendJson(HttpExchange ex, int code, JSONArray body) throws IOException {
        byte[] bytes = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse JSON en chaîne.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 * @param raw Le corps en chaîne
	 */
	protected void sendJson(HttpExchange ex, int code, String raw) throws IOException {
        byte[] bytes = raw.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse vide.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 */
	protected void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }

    /**
	 * Gère une exception.
	 * @param ex L'échange HTTP
	 * @param e L'exception
	 */
	protected void handleException(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace();
        sendJson(ex, 500, new JSONObject().put("error", e.getMessage()));
    }
}

/**
 * Contrôleur pour la gestion des utilisateurs.
 */
public final class UserController extends UserBaseController {

    private final UserRepository userRepo;

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	public UserController(Connection db) {
        super(db);
        this.userRepo = new UserRepository(db);
    }

    /**
	 * Route les requêtes.
	 * @param ex L'échange HTTP
	 */
	public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        AuthContext ctx = AuthContext.fromHeaders(ex);

        try {
            if (path.startsWith("/admin/users")) {
                if (!ctx.isAuthenticated() || !ctx.isAdmin()) {
                    sendJson(ex, 403, "{\"error\":\"Accès administrateur requis\"}");
                    return;
                }
                adminRoutes(ex, method, path);
                return;
            }

            if (!ctx.isAuthenticated()) {
                sendJson(ex, 401, "{\"error\":\"Authentification requise\"}");
                return;
            }

            if (path.startsWith("/users")) {
                userRoutes(ex, method, path, ctx);
                return;
            }

            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
        } catch (Exception e) {
            handleException(ex, e);
        }
    }

    /**
	 * Gère les routes admin.
	 * @param ex L'échange HTTP
	 * @param method La méthode HTTP
	 * @param path Le chemin
	 */
	private void adminRoutes(HttpExchange ex, String method, String path) throws Exception {
        int id = extractId(path);
        if ("GET".equals(method) && id == -1) {
            list(ex);
            return;
        }
        if ("POST".equals(method) && id == -1) {
            create(ex);
            return;
        }
        if (("PATCH".equals(method) || "PUT".equals(method)) && id != -1) {
            update(ex, id, true);
            return;
        }
        if ("DELETE".equals(method) && id != -1) {
            destroy(ex, id);
            return;
        }
        if ("GET".equals(method) && id != -1) {
            show(ex, id);
            return;
        }
        sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
    }

    /**
	 * Gère les routes utilisateur.
	 * @param ex L'échange HTTP
	 * @param method La méthode HTTP
	 * @param path Le chemin
	 * @param ctx Le contexte d'authentification
	 */
	private void userRoutes(HttpExchange ex, String method, String path, AuthContext ctx) throws Exception {
        int id = extractId(path);
        if (id == -1) {
            if ("GET".equals(method)) {
                if (!ctx.isAdmin()) {
                    sendJson(ex, 403, "{\"error\":\"Accès interdit\"}");
                    return;
                }
                list(ex);
                return;
            }
            if ("POST".equals(method)) {
                if (!ctx.isAdmin()) {
                    sendJson(ex, 403, "{\"error\":\"Accès interdit\"}");
                    return;
                }
                create(ex);
                return;
            }
            sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
            return;
        }
        if ("GET".equals(method)) {
            if (ctx.userId() != id && !ctx.isAdmin()) {
                sendJson(ex, 403, "{\"error\":\"Accès interdit\"}");
                return;
            }
            show(ex, id);
            return;
        }
        if (("PATCH".equals(method) || "PUT".equals(method))) {
            boolean allowAdmin = ctx.isAdmin();
            if (ctx.userId() != id && !allowAdmin) {
                sendJson(ex, 403, "{\"error\":\"Accès interdit\"}");
                return;
            }
            update(ex, id, allowAdmin);
            return;
        }
        if ("DELETE".equals(method)) {
            if (!ctx.isAdmin()) {
                sendJson(ex, 403, "{\"error\":\"Accès interdit\"}");
                return;
            }
            destroy(ex, id);
            return;
        }
        sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
    }

    /**
	 * Extrait l'identifiant du chemin.
	 * @param path Le chemin
	 * @return L'identifiant ou -1
	 */
	private int extractId(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3) {
            try {
                return Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    /**
	 * Liste les utilisateurs.
	 * @param ex L'échange HTTP
	 */
	private void list(HttpExchange ex) throws Exception {
        List<User> users = userRepo.list();
        users.sort(Comparator.comparing((User u) -> !u.isAdmin).thenComparing(u -> u.name.toLowerCase()));
        JSONArray arr = new JSONArray();
        for (User u : users) {
            arr.put(u.toJson());
        }
        sendJson(ex, 200, arr);
    }

    /**
	 * Affiche un utilisateur.
	 * @param ex L'échange HTTP
	 * @param id L'identifiant
	 */
	private void show(HttpExchange ex, int id) throws Exception {
        User user = userRepo.find(id);
        if (user == null) {
            sendJson(ex, 404, "{\"error\":\"Utilisateur introuvable\"}");
            return;
        }
        sendJson(ex, 200, user.toJson());
    }

    /**
	 * Crée un utilisateur.
	 * @param ex L'échange HTTP
	 */
	private void create(HttpExchange ex) throws Exception {
        JSONObject body = parseJson(ex);
        String name = body.optString("name", null);
        String email = body.optString("email", null);
        String password = body.optString("password", null);
        boolean admin = body.optBoolean("admin", false);

        if (email == null || password == null) {
            sendJson(ex, 400, "{\"error\":\"email et password sont requis\"}");
            return;
        }
        if (userRepo.findByEmail(email) != null) {
            sendJson(ex, 409, "{\"error\":\"Email déjà utilisé\"}");
            return;
        }

        User user = new User(0, name, email, PasswordUtil.hashPassword(password), admin, null);
        int id = userRepo.create(user);
        User created = userRepo.find(id);
        sendJson(ex, 201, created.toJson());
    }

    /**
	 * Met à jour un utilisateur.
	 * @param ex L'échange HTTP
	 * @param id L'identifiant
	 * @param allowAdminField Si modification du champ admin autorisée
	 */
	private void update(HttpExchange ex, int id, boolean allowAdminField) throws Exception {
        User user = userRepo.find(id);
        if (user == null) {
            sendJson(ex, 404, "{\"error\":\"Utilisateur introuvable\"}");
            return;
        }
        JSONObject body = parseJson(ex);
        if (body.has("name")) user.name = body.getString("name");
        if (body.has("email")) user.email = body.getString("email");
        if (body.has("password")) {
            String pwd = body.getString("password");
            if (!pwd.isBlank()) {
                user.passwordHash = PasswordUtil.hashPassword(pwd);
            }
        }
        if (allowAdminField && body.has("admin")) {
            user.isAdmin = body.getBoolean("admin");
        }
        userRepo.update(user);
        sendJson(ex, 200, userRepo.find(id).toJson());
    }

    /**
	 * Supprime un utilisateur.
	 * @param ex L'échange HTTP
	 * @param id L'identifiant
	 */
	private void destroy(HttpExchange ex, int id) throws Exception {
        userRepo.delete(id);
        sendEmpty(ex, 200);
    }
}
