// src/controllers/OrderController.java
package controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Order;
import model.OrderItem;
import model.PlantStock;
import model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.PlantRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class OrderController {

    private final OrderRepository repo;
    private final OrderItemRepository itemRepo;
    private final PlantRepository plantRepo;
    private final Connection db;
    private final HttpClient httpClient;
    private final String catalogServiceUrl;

    public OrderController(Connection db) {
        this(db, System.getenv().getOrDefault("CATALOG_SERVICE_URL", "http://localhost:4102"));
    }

    public OrderController(Connection db, String catalogUrl) {
        this.db = db;
        this.repo = new OrderRepository(db);
        this.itemRepo = new OrderItemRepository(db);
        this.plantRepo = new PlantRepository(db);
        this.catalogServiceUrl = catalogUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public void list(Context ctx) throws Exception {
        User currentUser = ctx.attribute("user");
        if (currentUser == null) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Non authentifié"));
            return;
        }

        List<Order> orders = repo.listByUser(currentUser.id);
        orders.sort(orderComparator());

        List<Map<String, Object>> payload = new ArrayList<>(orders.size());
        for (Order order : orders) {
            payload.add(toOrderJson(order, itemRepo.listByOrder(order.id)));
        }
        ctx.json(payload);
    }

    public void create(Context ctx) throws Exception {
        User currentUser = ctx.attribute("user");
        if (currentUser == null) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Non authentifié"));
            return;
        }

        JSONObject body = new JSONObject(ctx.body());
        if (!body.has("items")) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Le corps doit contenir un tableau items"));
            return;
        }

        JSONArray itemsJson = body.getJSONArray("items");
        if (itemsJson.length() == 0) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "La commande doit contenir au moins un item"));
            return;
        }

        db.setAutoCommit(false);
        try {
            Order newOrder = new Order(currentUser.id, BigDecimal.ZERO, "pending");
            int orderId = repo.create(newOrder);
            BigDecimal total = BigDecimal.ZERO;

            for (int i = 0; i < itemsJson.length(); i++) {
                total = total.add(createOrderItem(orderId, itemsJson.getJSONObject(i)));
            }
            repo.updateTotal(orderId, total);
            db.commit();

            Order finalOrder = repo.find(orderId);
            ctx.status(HttpStatus.CREATED).json(toOrderJson(finalOrder, itemRepo.listByOrder(orderId)));
        } catch (IllegalArgumentException ex) {
            db.rollback();
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.setAutoCommit(true);
        }
    }

    public void patch(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Order order = repo.find(id);
        if (order == null) throw new NotFoundResponse();

        JSONObject body = new JSONObject(ctx.body());
        if (body.has("status")) {
            repo.updateStatus(id, body.getString("status"));
        }

        Order updated = repo.find(id);
        ctx.json(toOrderJson(updated, itemRepo.listByOrder(id)));
    }

    public void destroy(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        itemRepo.deleteByOrder(id);
        repo.delete(id);
        ctx.status(HttpStatus.OK).json(Map.of("deleted", true));
    }

    private BigDecimal createOrderItem(int orderId, JSONObject itemJson) throws Exception {
        if (!itemJson.has("plantId") || !itemJson.has("quantity")) {
            throw new IllegalArgumentException("Chaque item doit contenir plantId et quantity");
        }
        int plantId = itemJson.getInt("plantId");
        int quantity = itemJson.getInt("quantity");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity doit être supérieur à 0");
        }

        PlantStock plant = plantRepo.find(plantId);
        if (plant == null) {
            throw new IllegalArgumentException("Plante " + plantId + " introuvable");
        }
        if (plant.stock < quantity) {
            throw new IllegalArgumentException("Stock insuffisant pour " + plant.name);
        }

        int newStock = plant.stock - quantity;
        boolean stockUpdated = updateCatalogStock(plantId, newStock);
        if (!stockUpdated) {
            throw new RuntimeException("Échec de la mise à jour du stock pour " + plant.name);
        }

        OrderItem item = new OrderItem(orderId, plantId, quantity, plant.price);
        itemRepo.create(item);
        return plant.price.multiply(BigDecimal.valueOf(quantity));
    }

    private boolean updateCatalogStock(int plantId, int newStock) {
        try {
            JSONObject body = new JSONObject().put("stock", newStock);
            String uri = String.format("%s/internal/plants/%d/stock", this.catalogServiceUrl, plantId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() >= 200 && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Map<String, Object> toOrderJson(Order order, List<OrderItem> items) throws Exception {
        List<Map<String, Object>> itemsJson = new ArrayList<>(items.size());
        for (OrderItem item : items) {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("id", item.id);
            itemMap.put("orderId", item.orderId);
            itemMap.put("plantId", item.plantId);
            itemMap.put("quantity", item.quantity);
            itemMap.put("price", item.price.doubleValue());

            PlantStock plant = plantRepo.find(item.plantId);
            if (plant != null) {
                Map<String, Object> plantMap = new LinkedHashMap<>();
                plantMap.put("id", plant.id);
                plantMap.put("name", plant.name);
                plantMap.put("price", plant.price.doubleValue());
                plantMap.put("stock", plant.stock);
                itemMap.put("plant", plantMap);
            }
            itemsJson.add(itemMap);
        }

        Map<String, Object> orderMap = new LinkedHashMap<>();
        orderMap.put("id", order.id);
        orderMap.put("userId", order.userId);
        orderMap.put("totalPrice", order.total.doubleValue());
        orderMap.put("status", order.status);
        orderMap.put("createdAt", order.createdAt == null ? null : order.createdAt.toInstant().atOffset(java.time.ZoneOffset.UTC).toString());
        orderMap.put("orderItems", itemsJson);
        return orderMap;
    }

    private Comparator<Order> orderComparator() {
        return (left, right) -> {
            if (left.createdAt == null || right.createdAt == null) {
                return Integer.compare(right.id, left.id);
            }
            return right.createdAt.compareTo(left.createdAt);
        };
    }
}
