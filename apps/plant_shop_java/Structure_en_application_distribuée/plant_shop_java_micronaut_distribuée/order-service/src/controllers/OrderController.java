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
import java.util.List;
import java.util.Map;
import models.Order;
import models.OrderItem;
import models.Plant;
import models.User;
import repositories.OrderItemRepository;
import repositories.OrderRepository;
import repositories.PlantRepository;
import security.Guards;
import util.ApiMapper;

@Controller("/api/orders")
public class OrderController {

    private final OrderRepository repo;
    private final OrderItemRepository itemRepo;
    private final PlantRepository plantRepo;
    private final Connection db;

    @Inject
    public OrderController(Connection db) {
        this.db = db;
        this.repo = new OrderRepository(db);
        this.itemRepo = new OrderItemRepository(db);
        this.plantRepo = new PlantRepository(db);
    }

    @Get
    public List<?> list(HttpRequest<?> request) throws Exception {
        User currentUser = Guards.requireUser(request);
        List<Order> orders = currentUser.isAdmin ? repo.list() : repo.listByUser(currentUser.id);
        orders.sort(Comparator.comparing(o -> o.createdAt, Comparator.reverseOrder()));

        List<Map<String, Object>> payload = new ArrayList<>();
        for (Order order : orders) {
            List<Map<String, Object>> items = ApiMapper.toOrderItems(itemRepo.listByOrder(order.id), plantRepo::find);
            payload.add(ApiMapper.toOrder(order, items));
        }
        return payload;
    }

    @Post
    public HttpResponse<?> create(@Body Map<String, List<Map<String, Integer>>> body, HttpRequest<?> request) throws Exception {
        User currentUser = Guards.requireUser(request);
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
            List<Map<String, Object>> items = ApiMapper.toOrderItems(itemRepo.listByOrder(orderId), plantRepo::find);
            return HttpResponse.created(ApiMapper.toOrder(finalOrder, items));
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

    @Patch("/{id}")
    public HttpResponse<?> patch(@PathVariable int id, @Body Map<String, String> body, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        if (repo.find(id) == null) return HttpResponse.notFound();
        if (body.containsKey("status")) {
            repo.updateStatus(id, body.get("status"));
        }
        Order updated = repo.find(id);
        List<Map<String, Object>> items = ApiMapper.toOrderItems(itemRepo.listByOrder(id), plantRepo::find);
        return HttpResponse.ok(ApiMapper.toOrder(updated, items));
    }

    @Delete("/{id}")
    public HttpResponse<?> destroy(@PathVariable int id, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        itemRepo.deleteByOrder(id);
        repo.delete(id);
        return HttpResponse.ok();
    }

    private BigDecimal createOrderItem(int orderId, Map<String, Integer> itemMap) throws Exception {
        int plantId = itemMap.get("plantId");
        int quantity = itemMap.get("quantity");
        Plant plant = plantRepo.find(plantId);
        if (plant == null) throw new IllegalArgumentException("Plante " + plantId + " introuvable");
        if (plant.stock < quantity) throw new IllegalArgumentException("Stock insuffisant pour " + plant.name);

        plantRepo.updateStock(plant.id, plant.stock - quantity);
        OrderItem item = new OrderItem(orderId, plantId, quantity, plant.price);
        itemRepo.create(item);
        return plant.price.multiply(BigDecimal.valueOf(quantity));
    }
}
