package controllers;

import model.Order;
import model.OrderItem;
import model.PlantStock;
import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.PlantRepository;
import security.Guards;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import util.EnvLoader;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    OrderRepository repo;
    @Autowired
    OrderItemRepository itemRepo;
    @Autowired
    PlantRepository plantRepo;
    @Autowired
    Guards guards;

    private final HttpClient httpClient;
    private final String catalogServiceUrl;

    public OrderController() {
        Map<String, String> env = EnvLoader.load();
        String host = env.getOrDefault("SERVICE_HOST", "http://localhost");
        String port = env.getOrDefault("CATALOG_SERVICE_PORT", "6102");
        this.catalogServiceUrl = host + ":" + port + "/api";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() throws Exception {
        User currentUser = guards.requireUser();
        List<Order> orders = repo.findByUserIdOrderByCreatedAtDesc(currentUser.id);

        List<Map<String, Object>> payload = new ArrayList<>();
        for (Order order : orders) {
            payload.add(toOrderJson(order, itemRepo.findByOrderId(order.id)));
        }
        return ResponseEntity.ok(payload);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> create(@RequestBody Map<String, List<Map<String, Integer>>> body) throws Exception {
        User currentUser = guards.requireUser();
        List<Map<String, Integer>> itemsJson = body.get("items");

        if (itemsJson == null || itemsJson.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "items requis"));
        }

        try {
            Order newOrder = new Order(currentUser.id, BigDecimal.ZERO, "pending");
            Order savedOrder = repo.save(newOrder);
            BigDecimal total = BigDecimal.ZERO;

            for (Map<String, Integer> it : itemsJson) {
                total = total.add(createOrderItem(savedOrder, it));
            }
            savedOrder.total = total;
            repo.save(savedOrder);

            return ResponseEntity.status(HttpStatus.CREATED)
                                 .body(toOrderJson(savedOrder, itemRepo.findByOrderId(savedOrder.id)));

        } catch (IllegalArgumentException ex) {
            // Gère les erreurs de stock ou d'ID
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> patch(@PathVariable("id") int id, @RequestBody Map<String, String> body) throws Exception {
        guards.requireAdmin();
        Order existing = repo.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (body.containsKey("status")) {
            existing.status = body.get("status");
            repo.save(existing);
        }

        return ResponseEntity.ok(toOrderJson(existing, itemRepo.findByOrderId(id)));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> destroy(@PathVariable("id") int id) throws Exception {
        guards.requireAdmin();
        // Doit supprimer les items avant la commande à cause de la clé étrangère
        itemRepo.deleteByOrderId(id);
        repo.deleteById(id);
        return ResponseEntity.ok().build(); // 200 OK attendu par le test
    }

    /**
     * Logique privée pour créer un item et mettre à jour le stock.
     */
    private BigDecimal createOrderItem(Order order, Map<String, Integer> itemMap) throws Exception {
        int plantId = itemMap.get("plantId");
        int quantity = itemMap.get("quantity");
        PlantStock plant = plantRepo.findById(plantId).orElse(null);

        if (plant == null) throw new IllegalArgumentException("Plante " + plantId + " introuvable");
        if (plant.stock < quantity) throw new IllegalArgumentException("Stock insuffisant pour " + plant.name);

        int newStock = plant.stock - quantity;
        boolean stockUpdated = updateCatalogStock(plantId, newStock);
        if (!stockUpdated) {
            throw new RuntimeException("Échec de la mise à jour du stock pour " + plant.name);
        }

        OrderItem item = new OrderItem(order.id, plantId, quantity, plant.price);
        itemRepo.save(item);

        return plant.price.multiply(BigDecimal.valueOf(quantity));
    }

    private boolean updateCatalogStock(int plantId, int newStock) {
        try {
            String json = String.format("{\"stock\":%d}", newStock);
            String uri = String.format("%s/internal/plants/%d/stock", this.catalogServiceUrl, plantId);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Content-Type", "application/json")
                    .method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

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

            PlantStock plant = plantRepo.findById(item.plantId).orElse(null);
            if (plant == null) {
                continue;
            }
            Map<String, Object> plantMap = new LinkedHashMap<>();
            plantMap.put("id", plant.id);
            plantMap.put("name", plant.name);
            plantMap.put("price", plant.price.doubleValue());
            plantMap.put("stock", plant.stock);
            itemMap.put("plant", plantMap);
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
}
