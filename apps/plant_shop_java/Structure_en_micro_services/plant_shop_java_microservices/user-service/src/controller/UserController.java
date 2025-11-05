package user.controller;

import user.model.User;
import user.repository.UserRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import util.AuthContext;
import util.PasswordUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.nio.charset.StandardCharsets;

// Définition locale de BaseController pour résoudre les dépendances du classpath lors de la compilation
abstract class UserBaseController implements HttpHandler {
    protected final Connection db;

    UserBaseController(Connection db) {
        this.db = db;
    }

    protected JSONObject parseJson(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return body.isBlank() ? new JSONObject() : new JSONObject(body);
    }

    protected void sendJson(HttpExchange ex, int code, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    protected void sendJson(HttpExchange ex, int code, JSONArray body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    protected void sendJson(HttpExchange ex, int code, String raw) throws IOException {
        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    protected void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }

    protected void handleException(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace();
        sendJson(ex, 500, new JSONObject().put("error", e.getMessage()));
    }

    @Override
    public abstract void handle(HttpExchange exchange) throws IOException;
}

public final class UserController extends UserBaseController {

    private final UserRepository userRepo;
    public UserController(Connection db) {
        super(db);
        this.userRepo = new UserRepository(db);
    }

    @Override
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

        if ("PATCH".equals(method) || "PUT".equals(method)) {
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

    private int extractId(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3) {
            try {
                return Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private void list(HttpExchange ex) throws Exception {
        List<User> users = userRepo.list();
        users.sort(Comparator.comparing((User u) -> !u.isAdmin()).thenComparing(u -> u.name().toLowerCase()));
        JSONArray arr = new JSONArray();
        for (User u : users) {
            arr.put(u.toJson());
        }
        sendJson(ex, 200, arr);
    }

    private void show(HttpExchange ex, int id) throws Exception {
        User user = userRepo.find(id);
        if (user == null) {
            sendJson(ex, 404, "{\"error\":\"Utilisateur introuvable\"}");
            return;
        }
        sendJson(ex, 200, user.toJson());
    }

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

        User toInsert = new User(0, name, email, PasswordUtil.hashPassword(password), admin, null);
        int id = userRepo.create(toInsert);
        sendJson(ex, 201, userRepo.find(id).toJson());
    }

    private void update(HttpExchange ex, int id, boolean allowAdminField) throws Exception {
        User user = userRepo.find(id);
        if (user == null) {
            sendJson(ex, 404, "{\"error\":\"Utilisateur introuvable\"}");
            return;
        }
        JSONObject body = parseJson(ex);
        User updated = user;
        if (body.has("name")) updated = updated.withName(body.getString("name"));
        if (body.has("email")) updated = updated.withEmail(body.getString("email"));
        if (body.has("password")) {
            String pwd = body.getString("password");
            if (!pwd.isBlank()) {
                updated = updated.withPasswordHash(PasswordUtil.hashPassword(pwd));
            }
        }
        if (allowAdminField && body.has("admin")) {
            updated = updated.withAdmin(body.getBoolean("admin"));
        }
        userRepo.update(updated);
        sendJson(ex, 200, userRepo.find(id).toJson());
    }

    private void destroy(HttpExchange ex, int id) throws Exception {
        userRepo.delete(id);
        sendEmpty(ex, 200);
    }
}
