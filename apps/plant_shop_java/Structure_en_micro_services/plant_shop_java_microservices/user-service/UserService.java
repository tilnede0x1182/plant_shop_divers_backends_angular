import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import util.AuthContext;
import util.PasswordUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UserService {

    private static Connection db;
    private static HttpServer server;

    public static void main(String[] args) throws Exception {
        Map<String, String> cfg = loadEnv();

        int port = Integer.parseInt(cfg.getOrDefault("USER_SERVICE_PORT", "6104"));
        String url = cfg.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/plant_shop_java_microservices");
        String user = cfg.get("DATABASE_USER");
        String pass = cfg.get("DATABASE_PASS");

        if (user == null || pass == null) {
            throw new IllegalStateException("DATABASE_USER et DATABASE_PASS doivent être définis.");
        }

        db = DriverManager.getConnection(url, user, pass);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new UserRoutes(db));
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.printf("👥 UserService disponible sur http://localhost:%d%n", port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) {
                server.stop(0);
            }
            if (db != null) {
                try {
                    db.close();
                } catch (SQLException ignored) {}
            }
        }));
    }

    private static Map<String, String> loadEnv() throws IOException {
        Map<String, String> values = new HashMap<>();
        readEnv(Path.of("../config/.env"), values);
        readEnv(Path.of(".env"), values);
        return values;
    }

    private static void readEnv(Path path, Map<String, String> values) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
            }
        }
    }
}

final class UserRoutes implements HttpHandler {

    private final UserController controller;

    UserRoutes(Connection db) {
        this.controller = new UserController(db);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        controller.handle(exchange);
    }
}

abstract class UserBaseController {
    protected final Connection db;

    UserBaseController(Connection db) {
        this.db = db;
    }

    protected JSONObject parseJson(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return body.isBlank() ? new JSONObject() : new JSONObject(body);
    }

    protected void sendJson(HttpExchange ex, int code, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    protected void sendJson(HttpExchange ex, int code, JSONArray body) throws IOException {
        byte[] bytes = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    protected void sendJson(HttpExchange ex, int code, String raw) throws IOException {
        byte[] bytes = raw.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
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
}

final class UserController extends UserBaseController {

    private final UserRepository userRepo;

    UserController(Connection db) {
        super(db);
        this.userRepo = new UserRepository(db);
    }

    void handle(HttpExchange ex) throws IOException {
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
        users.sort(Comparator.comparing((User u) -> !u.isAdmin).thenComparing(u -> u.name.toLowerCase()));
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

        User user = new User(0, name, email, PasswordUtil.hashPassword(password), admin, null);
        int id = userRepo.create(user);
        User created = userRepo.find(id);
        sendJson(ex, 201, created.toJson());
    }

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

    private void destroy(HttpExchange ex, int id) throws Exception {
        userRepo.delete(id);
        sendEmpty(ex, 200);
    }
}

final class User {
    int id;
    String name;
    String email;
    String passwordHash;
    boolean isAdmin;
    Instant createdAt;

    User(int id, String name, String email, String passwordHash, boolean isAdmin, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
    }

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("email", email);
        json.put("admin", isAdmin);
        if (createdAt != null) {
            json.put("createdAt", createdAt.toString());
        }
        return json;
    }
}

final class UserRepository {
    private final Connection db;

    UserRepository(Connection db) {
        this.db = db;
    }

    User find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    User findByEmail(String email) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    List<User> list() throws SQLException {
        List<User> out = new ArrayList<>();
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users")) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    int create(User user) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO users(name, email, password_hash, is_admin) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.name);
            ps.setString(2, user.email);
            ps.setString(3, user.passwordHash);
            ps.setBoolean(4, user.isAdmin);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    void update(User user) throws SQLException {
        boolean updatePassword = user.passwordHash != null && !user.passwordHash.isBlank();
        String sql = updatePassword
            ? "UPDATE users SET name=?, email=?, is_admin=?, password_hash=? WHERE id=?"
            : "UPDATE users SET name=?, email=?, is_admin=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, user.name);
            ps.setString(2, user.email);
            ps.setBoolean(3, user.isAdmin);
            if (updatePassword) {
                ps.setString(4, user.passwordHash);
                ps.setInt(5, user.id);
            } else {
                ps.setInt(4, user.id);
            }
            ps.executeUpdate();
        }
    }

    void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getBoolean("is_admin"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
        );
    }
}
