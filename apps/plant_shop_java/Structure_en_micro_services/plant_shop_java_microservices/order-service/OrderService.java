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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OrderService {

    private static Connection db;
    private static HttpServer server;

    public static void main(String[] args) throws Exception {
        Map<String, String> cfg = loadEnv();

        int port = Integer.parseInt(cfg.getOrDefault("ORDER_SERVICE_PORT", "6103"));
        String url = cfg.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/plant_shop_java_microservices");
        String user = cfg.get("DATABASE_USER");
        String pass = cfg.get("DATABASE_PASS");

        if (user == null || pass == null) {
            throw new IllegalStateException("DATABASE_USER et DATABASE_PASS doivent être définis.");
        }

        db = DriverManager.getConnection(url, user, pass);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new OrderRoutes(db));
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.printf("🧾 OrderService disponible sur http://localhost:%d%n", port);

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

final class OrderRoutes implements HttpHandler {

    private final OrderController controller;

    OrderRoutes(Connection db) {
        this.controller = new OrderController(db);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        controller.handle(exchange);
    }
}

abstract class OrderBaseController {
    protected final Connection db;

    OrderBaseController(Connection db) {
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

final class OrderController extends OrderBaseController {

    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final PlantRepository plantRepo;

    OrderController(Connection db) {
        super(db);
        this.orderRepo = new OrderRepository(db);
        this.itemRepo = new OrderItemRepository(db);
        this.plantRepo = new PlantRepository(db);
    }

    void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        AuthContext ctx = AuthContext.fromHeaders(ex);

        try {
            if (path.startsWith("/admin/orders")) {
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

            if (path.startsWith("/orders")) {
                userRoutes(ex, method, path, ctx);
                return;
            }

            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
        } catch (Exception e) {
            handleException(ex, e);
        }
    }

    private void userRoutes(HttpExchange ex, String method, String path, AuthContext ctx) throws Exception {
        int id = extractId(path);
        if ("GET".equals(method)) {
            if (id == -1) {
                list(ex, ctx);
            } else {
                show(ex, ctx, id);
            }
            return;
        }
        if ("PATCH".equals(method) && id != -1) {
            if (!ctx.isAdmin()) {
                sendJson(ex, 403, "{\"error\":\"Accès interdit\"}");
                return;
            }
            patch(ex, id);
            return;
        }
        if ("POST".equals(method) && id == -1) {
            create(ex, ctx);
            return;
        }
        sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
    }

    private void adminRoutes(HttpExchange ex, String method, String path) throws Exception {
        int id = extractId(path);
        if ("PATCH".equals(method)) {
            if (id == -1) {
                sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
                return;
            }
            patch(ex, id);
            return;
        }
        if ("DELETE".equals(method) && id != -1) {
            destroy(ex, id);
            return;
        }
        if ("GET".equals(method) && id != -1) {
            showAdmin(ex, id);
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

    private void list(HttpExchange ex, AuthContext ctx) throws Exception {
        List<Order> orders = orderRepo.listByUser(ctx.userId());
        orders.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        JSONArray arr = new JSONArray();
        for (Order order : orders) {
            arr.put(toJson(order, itemRepo.listByOrder(order.id)));
        }
        sendJson(ex, 200, arr);
    }

    private void show(HttpExchange ex, AuthContext ctx, int id) throws Exception {
        Order order = orderRepo.find(id);
        if (order == null || order.userId != ctx.userId()) {
            sendJson(ex, 404, "{\"error\":\"Commande introuvable\"}");
            return;
        }
        sendJson(ex, 200, toJson(order, itemRepo.listByOrder(id)));
    }

    private void showAdmin(HttpExchange ex, int id) throws Exception {
        Order order = orderRepo.find(id);
        if (order == null) {
            sendJson(ex, 404, "{\"error\":\"Commande introuvable\"}");
            return;
        }
        sendJson(ex, 200, toJson(order, itemRepo.listByOrder(id)));
    }

    private void create(HttpExchange ex, AuthContext ctx) throws Exception {
        JSONObject body = parseJson(ex);
        JSONArray items = body.optJSONArray("items");
        if (items == null || items.isEmpty()) {
            sendJson(ex, 400, "{\"error\":\"items requis\"}");
            return;
        }

        Order order = new Order(0, ctx.userId(), BigDecimal.ZERO, "pending", Instant.now());
        int orderId = orderRepo.create(order);
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < items.length(); i++) {
            JSONObject obj = items.getJSONObject(i);
            int plantId = obj.getInt("plantId");
            int quantity = obj.getInt("quantity");
            Plant plant = plantRepo.find(plantId);
            if (plant == null) {
                throw new IllegalArgumentException("Plante " + plantId + " introuvable");
            }
            if (plant.stock < quantity) {
                throw new IllegalArgumentException("Stock insuffisant pour " + plant.name);
            }
            plantRepo.updateStock(plantId, plant.stock - quantity);
            BigDecimal price = plant.price;
            total = total.add(price.multiply(BigDecimal.valueOf(quantity)));
            itemRepo.create(new OrderItem(0, orderId, plantId, quantity, price));
        }

        orderRepo.updateTotal(orderId, total);
        Order created = orderRepo.find(orderId);
        sendJson(ex, 201, toJson(created, itemRepo.listByOrder(orderId)));
    }

    private void patch(HttpExchange ex, int id) throws Exception {
        Order order = orderRepo.find(id);
        if (order == null) {
            sendJson(ex, 404, "{\"error\":\"Commande introuvable\"}");
            return;
        }
        JSONObject body = parseJson(ex);
        if (body.has("status")) {
            orderRepo.updateStatus(id, body.getString("status"));
        }
        Order updated = orderRepo.find(id);
        sendJson(ex, 200, toJson(updated, itemRepo.listByOrder(id)));
    }

    private void destroy(HttpExchange ex, int id) throws Exception {
        itemRepo.deleteByOrder(id);
        orderRepo.delete(id);
        sendEmpty(ex, 200);
    }

    private JSONObject toJson(Order order, List<OrderItem> items) throws SQLException {
        JSONArray itemsJson = new JSONArray();
        for (OrderItem item : items) {
            JSONObject obj = new JSONObject()
                .put("id", item.id)
                .put("orderId", item.orderId)
                .put("plantId", item.plantId)
                .put("quantity", item.quantity)
                .put("price", item.price);
            Plant plant = plantRepo.find(item.plantId);
            if (plant != null) {
                obj.put("plant", new JSONObject()
                    .put("id", plant.id)
                    .put("name", plant.name)
                    .put("price", plant.price));
            }
            itemsJson.put(obj);
        }

        return new JSONObject()
            .put("id", order.id)
            .put("userId", order.userId)
            .put("totalPrice", order.total)
            .put("status", order.status)
            .put("createdAt", order.createdAt.toString())
            .put("orderItems", itemsJson);
    }
}

final class Order {
    int id;
    int userId;
    BigDecimal total;
    String status;
    Instant createdAt;

    Order(int id, int userId, BigDecimal total, String status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }
}

final class OrderItem {
    int id;
    int orderId;
    int plantId;
    int quantity;
    BigDecimal price;

    OrderItem(int id, int orderId, int plantId, int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.plantId = plantId;
        this.quantity = quantity;
        this.price = price;
    }
}

final class Plant {
    int id;
    String name;
    BigDecimal price;
    int stock;

    Plant(int id, String name, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}

abstract class OrderBaseRepository<T> {
    protected final Connection db;
    protected final String table;

    OrderBaseRepository(Connection db, String table) {
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

    void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

final class OrderRepository extends OrderBaseRepository<Order> {

    OrderRepository(Connection db) {
        super(db, "orders");
    }

    @Override
    Order map(ResultSet rs) throws SQLException {
        return new Order(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    int create(Order order) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO orders(user_id, total, status) VALUES (?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.userId);
            ps.setBigDecimal(2, order.total);
            ps.setString(3, order.status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    void updateTotal(int id, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("UPDATE orders SET total=? WHERE id=?")) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    void updateStatus(int id, String status) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("UPDATE orders SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    List<Order> listByUser(int userId) throws SQLException {
        List<Order> out = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM orders WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }
}

final class OrderItemRepository extends OrderBaseRepository<OrderItem> {

    OrderItemRepository(Connection db) {
        super(db, "order_items");
    }

    @Override
    OrderItem map(ResultSet rs) throws SQLException {
        return new OrderItem(
            rs.getInt("id"),
            rs.getInt("order_id"),
            rs.getInt("plant_id"),
            rs.getInt("quantity"),
            rs.getBigDecimal("price")
        );
    }

    int create(OrderItem item) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO order_items(order_id, plant_id, quantity, price) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.orderId);
            ps.setInt(2, item.plantId);
            ps.setInt(3, item.quantity);
            ps.setBigDecimal(4, item.price);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    List<OrderItem> listByOrder(int orderId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM order_items WHERE order_id=?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    void deleteByOrder(int orderId) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM order_items WHERE order_id=?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}

final class PlantRepository {
    private final Connection db;

    PlantRepository(Connection db) {
        this.db = db;
    }

    Plant find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM plants WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Plant(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock")
                    );
                }
            }
        }
        return null;
    }

    void updateStock(int id, int stock) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("UPDATE plants SET stock=? WHERE id=?")) {
            ps.setInt(1, stock);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
