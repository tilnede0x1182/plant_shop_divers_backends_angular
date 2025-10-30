// src/controllers/OrderController.java
package controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import model.Order;
import model.OrderItem;
import model.Plant;
import model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.PlantRepository;
import util.ApiMapper;

public final class OrderController {

    private final OrderRepository repo;
    private final OrderItemRepository itemRepo;
    private final PlantRepository plantRepo;
    private final Connection db;

    public OrderController(Connection db) {
        this.db = db;
        this.repo = new OrderRepository(db);
        this.itemRepo = new OrderItemRepository(db);
        this.plantRepo = new PlantRepository(db);
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
            List<Map<String, Object>> items = ApiMapper.toOrderItems(itemRepo.listByOrder(order.id), plantRepo::find);
            payload.add(ApiMapper.toOrder(order, items));
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
            List<Map<String, Object>> items = ApiMapper.toOrderItems(itemRepo.listByOrder(orderId), plantRepo::find);
            ctx.status(HttpStatus.CREATED).json(ApiMapper.toOrder(finalOrder, items));
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
        List<Map<String, Object>> items = ApiMapper.toOrderItems(itemRepo.listByOrder(id), plantRepo::find);
        ctx.json(ApiMapper.toOrder(updated, items));
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

        Plant plant = plantRepo.find(plantId);
        if (plant == null) {
            throw new IllegalArgumentException("Plante " + plantId + " introuvable");
        }
        if (plant.stock < quantity) {
            throw new IllegalArgumentException("Stock insuffisant pour la plante " + plantId);
        }

        plantRepo.updateStock(plant.id, plant.stock - quantity);
        OrderItem item = new OrderItem(orderId, plantId, quantity, plant.price);
        itemRepo.create(item);
        return plant.price.multiply(BigDecimal.valueOf(quantity));
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
