package order.controller;

import order.model.Order;
import order.model.OrderItem;
import order.model.PlantStock;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

final class OrderItemController {

    private final PlantRepository plantRepo;

    OrderItemController(Connection db) {
        this.plantRepo = new PlantRepository(db);
    }

    JSONObject toJson(Order order, List<OrderItem> items) throws SQLException {
        JSONArray itemsJson = new JSONArray();
        for (OrderItem item : items) {
            JSONObject obj = new JSONObject()
                .put("id", item.id())
                .put("orderId", item.orderId())
                .put("plantId", item.plantId())
                .put("quantity", item.quantity())
                .put("price", item.price());

            PlantStock plant = plantRepo.find(item.plantId());
            if (plant != null) {
                obj.put("plant", new JSONObject()
                    .put("id", plant.id())
                    .put("name", plant.name())
                    .put("price", plant.price()));
            }
            itemsJson.put(obj);
        }

        return new JSONObject()
            .put("id", order.id())
            .put("userId", order.userId())
            .put("totalPrice", order.total())
            .put("status", order.status())
            .put("createdAt", order.createdAt().toString())
            .put("orderItems", itemsJson);
    }
}
