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
import java.util.ArrayList;
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
                Map<String, Object> orderMap = order.toMap();
                LazyList<OrderItem> items = order.getAll(OrderItem.class);
                List<Map<String, Object>> itemMaps = new ArrayList<>();
                for (OrderItem item : items) {
                    Map<String, Object> itemMap = item.toMap();
                    Plant plant = item.parent(Plant.class);
                    if (plant != null) {
                        itemMap.put("plant", plant.toMap());
                    }
                    itemMaps.add(itemMap);
                }
                orderMap.put("orderItems", itemMaps);
                payload.add(orderMap);
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
            respondJson(201, created.toJson(false));
        });
    }

    @PATCH
    public void update(int id) {
        runAction(() -> {
            requireAdmin();
            Order order = Order.findById(id);
            if (order == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Commande introuvable")));
                return;
            }
            Map<String, Object> params = ApiMapper.jsonToMap(getRequestString());
            Object status = params.get("status");
            if (status instanceof String) {
                order.set("status", status).saveIt();
            }
            respondJson(200, order.toJson(false));
        });
    }

    @DELETE
    public void destroy(int id) {
        runAction(() -> {
            requireAdmin();
            Order order = Order.findById(id);
            if (order == null) {
                respondJson(404, JsonHelper.toJsonString(Map.of("error", "Commande introuvable")));
                return;
            }
            order.deleteCascade();
            respondEmpty(200);
        });
    }
}
