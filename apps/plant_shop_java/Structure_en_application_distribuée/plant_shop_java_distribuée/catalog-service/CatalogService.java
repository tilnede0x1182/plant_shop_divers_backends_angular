import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import util.AuthContext;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
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
import java.util.Locale;
import java.util.Map;

public final class CatalogService {

    private static Connection db;
    private static HttpServer server;

    public static void main(String[] args) throws Exception {
        Map<String, String> cfg = loadEnv();

        int port = Integer.parseInt(cfg.getOrDefault("CATALOG_SERVICE_PORT", "6102"));
        String url = cfg.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/plant_shop_java_microservices");
        String user = cfg.get("DATABASE_USER");
        String pass = cfg.get("DATABASE_PASS");

        if (user == null || pass == null) {
            throw new IllegalStateException("DATABASE_USER et DATABASE_PASS doivent être définis.");
        }

        db = DriverManager.getConnection(url, user, pass);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new CatalogRoutes(db));
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.printf("🌱 CatalogService disponible sur http://localhost:%d%n", port);

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

final class CatalogRoutes implements HttpHandler {

    private final PlantController controller;

    CatalogRoutes(Connection db) {
        this.controller = new PlantController(db);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        controller.handle(ex);
    }
}

abstract class CatalogBaseController {
    protected final Connection db;

    CatalogBaseController(Connection db) {
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

    protected void sendJson(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
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

final class PlantController extends CatalogBaseController {

    private final PlantRepository repo;
    private final Comparator<Plant> nameComparator;

    PlantController(Connection db) {
        super(db);
        this.repo = new PlantRepository(db);
        java.text.Collator col = java.text.Collator.getInstance(Locale.ROOT);
        col.setStrength(java.text.Collator.PRIMARY);
        this.nameComparator = (a, b) -> col.compare(a.name, b.name);
    }

    void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        AuthContext ctx = AuthContext.fromHeaders(ex);

        try {
            if (path.startsWith("/admin/plants")) {
                if (!ctx.isAuthenticated() || !ctx.isAdmin()) {
                    sendJson(ex, 403, "{\"error\":\"Accès administrateur requis\"}");
                    return;
                }
                handleAdmin(ex, method, path);
                return;
            }

            if (path.startsWith("/plants")) {
                handlePublic(ex, method, path, ctx);
                return;
            }

            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
        } catch (Exception e) {
            handleException(ex, e);
        }
    }

    private void handlePublic(HttpExchange ex, String method, String path, AuthContext ctx) throws Exception {
        if ("GET".equals(method)) {
            int id = extractId(path);
            if (id == -1) {
                list(ex);
            } else {
                show(ex, id);
            }
            return;
        }
        sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
    }

    private void handleAdmin(HttpExchange ex, String method, String path) throws Exception {
        int id = extractId(path);
        if ("GET".equals(method)) {
            if (id == -1) {
                list(ex);
            } else {
                show(ex, id);
            }
            return;
        }
        if ("POST".equals(method) && id == -1) {
            create(ex);
            return;
        }
        if ("PATCH".equals(method) && id != -1) {
            update(ex, id);
            return;
        }
        if ("DELETE".equals(method) && id != -1) {
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
        List<Plant> plants = repo.list();
        plants.sort(nameComparator);
        JSONArray array = new JSONArray();
        for (Plant plant : plants) {
            array.put(plant.toJson());
        }
        sendJson(ex, 200, array);
    }

    private void show(HttpExchange ex, int id) throws Exception {
        Plant plant = repo.find(id);
        if (plant == null) {
            sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
            return;
        }
        sendJson(ex, 200, plant.toJson());
    }

    private void create(HttpExchange ex) throws Exception {
        JSONObject body = parseJson(ex);
        String name = body.optString("name", null);
        BigDecimal price = body.has("price") ? body.getBigDecimal("price") : null;
        if (name == null || price == null) {
            sendJson(ex, 400, "{\"error\":\"name et price sont requis\"}");
            return;
        }

        Plant plant = new Plant(
            0,
            name,
            body.optString("description", ""),
            price,
            body.optInt("stock", 0),
            null
        );
        int id = repo.create(plant);
        plant.id = id;
        sendJson(ex, 201, plant.toJson());
    }

    private void update(HttpExchange ex, int id) throws Exception {
        Plant existing = repo.find(id);
        if (existing == null) {
            sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
            return;
        }
        JSONObject body = parseJson(ex);
        if (body.has("name")) existing.name = body.getString("name");
        if (body.has("description")) existing.description = body.getString("description");
        if (body.has("price")) existing.price = body.getBigDecimal("price");
        if (body.has("stock")) existing.stock = body.getInt("stock");
        repo.update(existing);
        sendJson(ex, 200, existing.toJson());
    }

    private void destroy(HttpExchange ex, int id) throws Exception {
        repo.delete(id);
        sendEmpty(ex, 200);
    }
}

final class Plant {
    int id;
    String name;
    String description;
    BigDecimal price;
    int stock;
    Instant createdAt;

    Plant(int id, String name, String description, BigDecimal price, int stock, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", description == null ? JSONObject.NULL : description);
        json.put("price", price);
        json.put("stock", stock);
        if (createdAt != null) {
            json.put("createdAt", createdAt.toString());
        }
        return json;
    }
}

abstract class CatalogBaseRepository<T> {
    protected final Connection db;
    protected final String table;

    CatalogBaseRepository(Connection db, String table) {
        this.db = db;
        this.table = table;
    }

    abstract T map(ResultSet rs) throws SQLException;

    T find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    List<T> list() throws SQLException {
        List<T> out = new ArrayList<>();
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + table)) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

final class PlantRepository extends CatalogBaseRepository<Plant> {

    PlantRepository(Connection db) {
        super(db, "plants");
    }

    @Override
    Plant map(ResultSet rs) throws SQLException {
        return new Plant(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getBigDecimal("price"),
            rs.getInt("stock"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
        );
    }

    int create(Plant plant) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO plants(name, description, price, stock) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plant.name);
            ps.setString(2, plant.description);
            ps.setBigDecimal(3, plant.price);
            ps.setInt(4, plant.stock);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    void update(Plant plant) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "UPDATE plants SET name=?, description=?, price=?, stock=? WHERE id=?")) {
            ps.setString(1, plant.name);
            ps.setString(2, plant.description);
            ps.setBigDecimal(3, plant.price);
            ps.setInt(4, plant.stock);
            ps.setInt(5, plant.id);
            ps.executeUpdate();
        }
    }
}
