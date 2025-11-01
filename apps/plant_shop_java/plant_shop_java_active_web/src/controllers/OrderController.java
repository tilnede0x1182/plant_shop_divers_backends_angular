package app.controllers;

import models.Order;
import models.OrderItem;
import models.Plant;
import models.User;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.javalite.activeweb.annotations.DELETE;
import org.javalite.activeweb.annotations.GET;
import org.javalite.activeweb.annotations.PATCH;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.ApiMapper;

public final class OrderController extends AppController {

    @GET
    public void index() {
        runAction(() -> {
            requireLogin();
            User user = getCurrentUser();
            LazyList<Order> orders = Order.where("user_id = ?", user.getId()).orderBy("created_at desc");

            List<Map<String, Object>> payload = new ArrayList<>();
            for (Order order : orders) {
                payload.add(serializeOrder(order));
            }

            respondJson(200, JsonHelper.toJsonString(payload));
        });
    }

    @POST
    public void create() {
        runAction(() -> {
            requireLogin();
            User user = getCurrentUser();
            Map<String, Object> body = ApiMapper.jsonToMap(getRequestString());
            List<Map<String, Object>> itemsData = ApiMapper.jsonToListOfMaps(body.get("items"));

            if (itemsData == null || itemsData.isEmpty()) {
                throw new IllegalArgumentException("Le panier est vide.");
            }

            Base.openTransaction();
            Order order = new Order();
            try {
                order.set("user_id", user.getId());
                order.set("status", "pending");
                order.set("total", BigDecimal.ZERO);
                order.saveIt();

                BigDecimal total = BigDecimal.ZERO;
                for (Map<String, Object> item : itemsData) {
                    int plantId = ((Number) item.get("plantId")).intValue();
                    int quantity = ((Number) item.get("quantity")).intValue();

                    Plant plant = Plant.findById(plantId);
                    if (plant == null) {
                        throw new IllegalArgumentException("Plante introuvable: " + plantId);
                    }
                    int stock = plant.getInteger("stock");
                    if (stock < quantity) {
                        throw new IllegalArgumentException("Stock insuffisant pour " + plant.getString("name"));
                    }

                    BigDecimal price = plant.getBigDecimal("price");
                    OrderItem.createIt(
                        "order_id", order.getId(),
                        "plant_id", plantId,
                        "quantity", quantity,
                        "price", price
                    );
                    plant.set("stock", stock - quantity).saveIt();
                    total = total.add(price.multiply(BigDecimal.valueOf(quantity)));
                }

                order.set("total", total).saveIt();
                Base.commitTransaction();
            } catch (Exception e) {
                Base.rollbackTransaction();
                throw e;
            }

            Order created = Order.findById(order.getId());
            respondJson(201, JsonHelper.toJsonString(serializeOrder(created)));
        });
    }

    @PATCH
    public void update() {
        runAction(() -> {
            requireAdmin();
            Integer orderId = parseId(getId());
            Order order = (orderId == null) ? null : Order.findById(orderId);
            if (order == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Commande introuvable")));
                return;
            }
            Map<String, Object> params = ApiMapper.jsonToMap(getRequestString());
            Object status = params.get("status");
            if (status instanceof String) {
                order.set("status", status).saveIt();
            }
            respondJson(200, JsonHelper.toJsonString(serializeOrder(order)));
        });
    }

    @DELETE
    public void destroy() {
        runAction(() -> {
            requireAdmin();
            Integer orderId = parseId(getId());
            Order order = (orderId == null) ? null : Order.findById(orderId);
            if (order == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Commande introuvable")));
                return;
            }
            order.deleteCascade();
            respondEmpty(200);
        });
    }

    private Integer parseId(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> serializeOrder(Order order) {
        Map<String, Object> orderMap = new LinkedHashMap<>();
        orderMap.put("id", order.getLongId());
        orderMap.put("userId", order.getInteger("user_id"));
        orderMap.put("status", order.getString("status"));
        orderMap.put("totalPrice", toDecimal(order.getBigDecimal("total")));
        orderMap.put("createdAt", toIso(order.getTimestamp("created_at")));
        orderMap.put("orderItems", serializeItems(order));
        return orderMap;
    }

    private List<Map<String, Object>> serializeItems(Order order) {
        LazyList<OrderItem> items = order.getAll(OrderItem.class);
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (OrderItem item : items) {
            Plant plant = item.parent(Plant.class);
            if (plant == null) {
                continue;
            }
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("id", item.getLongId());
            itemMap.put("orderId", item.getInteger("order_id"));
            itemMap.put("plantId", item.getInteger("plant_id"));
            itemMap.put("quantity", item.getInteger("quantity"));
            itemMap.put("price", toDecimal(item.getBigDecimal("price")));
            itemMap.put("plant", serializePlant(plant));
            serialized.add(itemMap);
        }
        return serialized;
    }

    private Map<String, Object> serializePlant(Plant plant) {
        Map<String, Object> plantMap = new LinkedHashMap<>();
        plantMap.put("id", plant.getLongId());
        plantMap.put("name", plant.getString("name"));
        plantMap.put("price", toDecimal(plant.getBigDecimal("price")));
        plantMap.put("stock", plant.getInteger("stock"));
        plantMap.put("description", plant.getString("description"));
        return plantMap;
    }

    private Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
}
