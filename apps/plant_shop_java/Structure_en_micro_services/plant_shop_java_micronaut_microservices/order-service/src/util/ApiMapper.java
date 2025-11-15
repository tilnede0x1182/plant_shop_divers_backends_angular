package util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import model.Order;
import model.OrderItem;
import model.PlantStock;

/**
 * ApiMapper local pour order-service avec uniquement les méthodes nécessaires
 */
public final class ApiMapper {

    private ApiMapper() {
        // utilitaire statique
    }

    public static Map<String, Object> toOrder(Order order, java.util.List<Map<String, Object>> items) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.id);
        map.put("userId", order.userId);
        map.put("totalPrice", toDecimal(order.total));
        map.put("status", order.status);
        map.put("createdAt", toIso(order.createdAt));
        map.put("orderItems", items);
        return map;
    }

    public static Map<String, Object> toOrderItem(OrderItem item, PlantStock plant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.id);
        map.put("orderId", item.orderId);
        map.put("plantId", item.plantId);
        map.put("quantity", item.quantity);
        map.put("price", toDecimal(item.price));
        if (plant != null) {
            Map<String, Object> plantMap = new LinkedHashMap<>();
            plantMap.put("id", plant.id);
            plantMap.put("name", plant.name);
            plantMap.put("price", toDecimal(plant.price));
            plantMap.put("stock", plant.stock);
            map.put("plant", plantMap);
        }
        return map;
    }

    private static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
}
