package controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import model.Order;
import model.OrderItem;
import model.PlantStock;
import model.UserDTO;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.PlantRepository;
import security.Guards;
import util.EnvLoader;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des commandes.
 */
@Controller("/orders")
public class OrderController {

    private final OrderRepository repo;
    private final OrderItemRepository itemRepo;
    private final PlantRepository plantRepo;
    private final Connection db;
    private final HttpClient httpClient;
    private final String catalogServiceUrl;

    /**
     * Construit le contrôleur avec la connexion BDD.
     * @param db Connexion à la base de données
     */
    @Inject
    public OrderController(Connection db) {
        this.db = db;
        this.repo = new OrderRepository(db);
        this.itemRepo = new OrderItemRepository(db);
        this.plantRepo = new PlantRepository(db);
        Map<String, String> env = EnvLoader.load();
        String host = env.getOrDefault("SERVICE_HOST", "http://localhost");
        String catalogPort = env.getOrDefault("CATALOG_SERVICE_PORT", "6102");
        this.catalogServiceUrl = env.getOrDefault("CATALOG_INTERNAL_URL", host + ":" + catalogPort);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Liste les commandes de l'utilisateur authentifié.
     * @param request Requête HTTP
     * @return Liste des commandes
     * @throws Exception En cas d'erreur BDD
     */
    @Get
    public List<?> list(HttpRequest<?> request) throws Exception {
        UserDTO currentUser = Guards.requireUser(request);
        List<Order> orders = currentUser.isAdmin ? repo.list() : repo.listByUser(currentUser.id);
        orders.sort(Comparator.comparing(o -> o.createdAt, Comparator.reverseOrder()));

        List<Map<String, Object>> payload = new ArrayList<>();
        for (Order order : orders) {
            payload.add(toOrderJson(order, itemRepo.listByOrder(order.id)));
        }
        return payload;
    }

    /**
     * Liste toutes les commandes (admin).
     * @param request Requête HTTP
     * @return Liste des commandes
     * @throws Exception En cas d'erreur BDD
     */
    @Get("/admin/orders")
    public List<?> listAdmin(HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        return list(request);
    }

    /**
     * Crée une nouvelle commande.
     * @param body Corps de la requête
     * @param request Requête HTTP
     * @return Réponse HTTP
     * @throws Exception En cas d'erreur BDD
     */
    @Post
    public HttpResponse<?> create(@Body Map<String, List<Map<String, Integer>>> body, HttpRequest<?> request) throws Exception {
        UserDTO currentUser = Guards.requireUser(request);
        List<Map<String, Integer>> itemsJson = body.get("items");
        if (itemsJson == null || itemsJson.isEmpty()) {
            return HttpResponse.badRequest(Map.of("error", "items requis"));
        }

        db.setAutoCommit(false);
        try {
            Order newOrder = new Order(currentUser.id, BigDecimal.ZERO, "pending");
            int orderId = repo.create(newOrder);
            BigDecimal total = BigDecimal.ZERO;

            for (Map<String, Integer> it : itemsJson) {
                total = total.add(createOrderItem(orderId, it));
            }
            repo.updateTotal(orderId, total);
            db.commit();

            Order finalOrder = repo.find(orderId);
            return HttpResponse.created(toOrderJson(finalOrder, itemRepo.listByOrder(orderId)));
        } catch (IllegalArgumentException ex) {
            db.rollback();
            return HttpResponse.badRequest(Map.of("error", ex.getMessage()));
        } catch (Exception e) {
            db.rollback();
            throw e;
        } finally {
            db.setAutoCommit(true);
        }
    }

    /**
     * Met à jour le statut d'une commande (admin).
     * @param id Identifiant de la commande
     * @param body Corps de la requête
     * @param request Requête HTTP
     * @return Réponse HTTP
     * @throws Exception En cas d'erreur BDD
     */
    @Patch("/{id}")
    public HttpResponse<?> patch(@PathVariable int id, @Body Map<String, String> body, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        if (repo.find(id) == null) return HttpResponse.notFound();
        if (body.containsKey("status")) {
            repo.updateStatus(id, body.get("status"));
        }
        Order updated = repo.find(id);
        return HttpResponse.ok(toOrderJson(updated, itemRepo.listByOrder(id)));
    }

    /**
     * Met à jour une commande via route admin.
     * @param id Identifiant de la commande
     * @param body Corps de la requête
     * @param request Requête HTTP
     * @return Réponse HTTP
     * @throws Exception En cas d'erreur BDD
     */
    @Patch("/admin/orders/{id}")
    public HttpResponse<?> patchAdmin(@PathVariable int id, @Body Map<String, String> body, HttpRequest<?> request) throws Exception {
        return patch(id, body, request);
    }

    /**
     * Supprime une commande (admin).
     * @param id Identifiant de la commande
     * @param request Requête HTTP
     * @return Réponse HTTP
     * @throws Exception En cas d'erreur BDD
     */
    @Delete("/{id}")
    public HttpResponse<?> destroy(@PathVariable int id, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        itemRepo.deleteByOrder(id);
        repo.delete(id);
        return HttpResponse.ok();
    }

    /**
     * Supprime une commande via route admin.
     * @param id Identifiant de la commande
     * @param request Requête HTTP
     * @return Réponse HTTP
     * @throws Exception En cas d'erreur BDD
     */
    @Delete("/admin/orders/{id}")
    public HttpResponse<?> destroyAdmin(@PathVariable int id, HttpRequest<?> request) throws Exception {
        return destroy(id, request);
    }

    /**
     * Crée un item de commande et met à jour le stock.
     * @param orderId ID de la commande parente
     * @param itemMap Données de l'item
     * @return Montant total de l'item
     * @throws Exception En cas d'erreur
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

    /**
     * Met à jour le stock via le service catalog.
     * @param plantId ID de la plante
     * @param newStock Nouveau stock
     * @return true si succès
     */
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

    /**
     * Convertit une commande en Map JSON.
     * @param order Commande à convertir
     * @param items Items de la commande
     * @return Map représentant le JSON
     * @throws Exception En cas d'erreur
     */
    private Map<String, Object> toOrderJson(Order order, List<OrderItem> items) throws Exception {
        List<Map<String, Object>> itemsJson = new ArrayList<>(items.size());
        for (OrderItem item : items) {
            PlantStock plant = plantRepo.find(item.plantId);
            if (plant == null) {
                continue;
            }

            Map<String, Object> plantMap = new LinkedHashMap<>();
            plantMap.put("id", plant.id);
            plantMap.put("name", plant.name);
            plantMap.put("price", plant.price.doubleValue());
            plantMap.put("stock", plant.stock);

            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("id", item.id);
            itemMap.put("orderId", item.orderId);
            itemMap.put("plantId", item.plantId);
            itemMap.put("quantity", item.quantity);
            itemMap.put("price", item.price.doubleValue());
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
