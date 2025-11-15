package controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Order;
import model.OrderItem;
import model.PlantStock;
import model.UserDTO;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.PlantRepository;
import security.Guards;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class OrderController {

    @Inject
    OrderRepository repo;
    @Inject
    OrderItemRepository itemRepo;
    @Inject
    PlantRepository plantRepo;
    @Inject
    Guards guards;

    private final HttpClient httpClient;
    private final String catalogServiceUrl;

    public OrderController() {
        this.catalogServiceUrl = System.getenv().getOrDefault("CATALOG_SERVICE_URL", "http://localhost:4302");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @GET
    public Response list() throws Exception {
        UserDTO currentUser = guards.requireUser();
        List<Order> orders = repo.listByUser(currentUser.id);

        orders.sort(Comparator.comparing(o -> o.createdAt, Comparator.reverseOrder()));

        List<Map<String, Object>> payload = new ArrayList<>();
        for (Order order : orders) {
            payload.add(toOrderJson(order, itemRepo.listByOrder(order.id)));
        }
        return Response.ok(payload).build();
    }

    @POST
    @Transactional // Gère la transaction (commit/rollback)
    public Response create(Map<String, List<Map<String, Integer>>> body) throws Exception {
        UserDTO currentUser = guards.requireUser();
        List<Map<String, Integer>> itemsJson = body.get("items");

        if (itemsJson == null || itemsJson.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "items requis"))
                           .build();
        }

        try {
            Order newOrder = new Order(currentUser.id, BigDecimal.ZERO, "pending");
            int orderId = repo.create(newOrder);
            BigDecimal total = BigDecimal.ZERO;

            for (Map<String, Integer> it : itemsJson) {
                total = total.add(createOrderItem(orderId, it));
            }
            repo.updateTotal(orderId, total);

            Order finalOrder = repo.find(orderId);

            return Response.status(Response.Status.CREATED)
                           .entity(toOrderJson(finalOrder, itemRepo.listByOrder(orderId)))
                           .build();

        } catch (IllegalArgumentException ex) {
            // @Transactional gère le rollback, mais on envoie la 400
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", ex.getMessage()))
                           .build();
        }
        // Les autres exceptions lèveront une 500 et @Transactional gèrera le rollback
    }

    @PATCH
    @Path("/{id}")
    @Transactional
    public Response patch(@PathParam("id") int id, Map<String, String> body) throws Exception {
        guards.requireAdmin();
        if (repo.find(id) == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (body.containsKey("status")) {
            repo.updateStatus(id, body.get("status"));
        }

        Order updated = repo.find(id);
        return Response.ok(toOrderJson(updated, itemRepo.listByOrder(id))).build();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response destroy(@PathParam("id") int id) throws Exception {
        guards.requireAdmin();
        // Doit supprimer les items avant la commande à cause de la clé étrangère
        itemRepo.deleteByOrder(id);
        repo.delete(id);
        return Response.ok().build(); // 200 OK attendu par le test
    }

    /**
     * Logique privée pour créer un item et mettre à jour le stock.
     */
    private BigDecimal createOrderItem(int orderId, Map<String, Integer> itemMap) throws Exception {
        int plantId = itemMap.get("plantId");
        int quantity = itemMap.get("quantity");
        PlantStock plant = plantRepo.find(plantId);

        if (plant == null) throw new IllegalArgumentException("Plante " + plantId + " introuvable");
        if (plant.stock < quantity) throw new IllegalArgumentException("Stock insuffisant pour " + plant.name);

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
}
