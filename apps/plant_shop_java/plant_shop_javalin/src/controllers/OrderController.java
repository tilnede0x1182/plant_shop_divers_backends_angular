// src/controllers/OrderController.java
package controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import model.Order;
import model.OrderItem;
import model.Plant;
import model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.PlantRepository;

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
        List<Order> orders = repo.list();
        orders.sort((o1, o2) -> o2.createdAt.compareTo(o1.createdAt));

        JSONArray result = new JSONArray();
        for (Order order : orders) {
            if (order.userId == currentUser.id) {
                result.put(toJson(order));
            }
        }
        ctx.json(result.toString());
    }

    public void create(Context ctx) throws Exception {
        User currentUser = ctx.attribute("user");
        JSONObject body = new JSONObject(ctx.body());
        JSONArray itemsJson = body.getJSONArray("items");

        db.setAutoCommit(false); // Début de la transaction
        try {
            Order newOrder = new Order(currentUser.id, BigDecimal.ZERO, "pending");
            int orderId = repo.create(newOrder);
            BigDecimal total = BigDecimal.ZERO;

            for (int i = 0; i < itemsJson.length(); i++) {
                total = total.add(createOrderItem(orderId, itemsJson.getJSONObject(i)));
            }
            repo.updateTotal(orderId, total);
            db.commit(); // Fin de la transaction

            Order finalOrder = repo.find(orderId);
            ctx.status(HttpStatus.CREATED).json(toJson(finalOrder));
        } catch (Exception e) {
            db.rollback(); // Annuler en cas d'erreur
            throw e;
        } finally {
            db.setAutoCommit(true);
        }
    }

    private BigDecimal createOrderItem(int orderId, JSONObject itemJson) throws SQLException {
        int plantId = itemJson.getInt("plantId");
        int quantity = itemJson.getInt("quantity");
        Plant plant = plantRepo.find(plantId);

        if (plant == null || plant.stock < quantity) {
            throw new SQLException("Stock insuffisant pour la plante " + plantId);
        }

        plantRepo.updateStock(plant.id, plant.stock - quantity);
        OrderItem item = new OrderItem(orderId, plantId, quantity, plant.price);
        itemRepo.create(item);
        return plant.price.multiply(new BigDecimal(quantity));
    }

    public void patch(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Order order = repo.find(id);
        if (order == null) throw new NotFoundResponse();

        JSONObject body = new JSONObject(ctx.body());
        if (body.has("status")) {
            repo.updateStatus(id, body.getString("status"));
        }
        ctx.json(toJson(repo.find(id)));
    }

    public void destroy(Context ctx) throws Exception {
        int id = Integer.parseInt(ctx.pathParam("id"));
        itemRepo.deleteByOrder(id);
        repo.delete(id);
        ctx.status(HttpStatus.OK);
    }

    private JSONObject toJson(Order o) throws SQLException {
        JSONObject json = new JSONObject();
        json.put("id", o.id);
        json.put("userId", o.userId);
        json.put("totalPrice", o.total); // Le test attend `totalPrice`
        json.put("status", o.status);
        json.put("createdAt", o.createdAt.toInstant().toString());

        JSONArray itemsArray = new JSONArray();
        for (OrderItem it : itemRepo.listByOrder(o.id)) {
            itemsArray.put(itemToJson(it));
        }
        json.put("orderItems", itemsArray);
        return json;
    }

    private JSONObject itemToJson(OrderItem it) throws SQLException {
        JSONObject itemJson = new JSONObject();
        itemJson.put("id", it.id);
        itemJson.put("plantId", it.plantId);
        itemJson.put("quantity", it.quantity);

        Plant p = plantRepo.find(it.plantId);
        if (p != null) {
            JSONObject plantJson = new JSONObject();
            plantJson.put("id", p.id);
            plantJson.put("name", p.name);
            plantJson.put("price", p.price);
            itemJson.put("plant", plantJson);
        }
        return itemJson;
    }
}
