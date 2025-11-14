package order.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Order;
import model.OrderItem;
import model.Plant;

public final class ApiMapper {

    private ApiMapper() {
        // utilitaire statique
    }

    public static Map<String, Object> toOrder(Order order, List<Map<String, Object>> items) {
        Map<String, Object> map = base();
        map.put("id", order.id);
        map.put("userId", order.userId);
        map.put("totalPrice", toDecimal(order.total));
        map.put("status", order.status);
        map.put("createdAt", toIso(order.createdAt));
        map.put("orderItems", items);
        return map;
    }

    public static Map<String, Object> toOrderItem(OrderItem item, Plant plant) {
        Map<String, Object> map = base();
        map.put("id", item.id);
        map.put("orderId", item.orderId);
        map.put("plantId", item.plantId);
        map.put("quantity", item.quantity);
        map.put("price", toDecimal(item.price));
        if (plant != null) {
            Map<String, Object> plantMap = base();
            plantMap.put("id", plant.id);
            plantMap.put("name", plant.name);
            plantMap.put("description", plant.description);
            plantMap.put("price", toDecimal(plant.price));
            plantMap.put("stock", plant.stock);
            plantMap.put("createdAt", toIso(plant.createdAt));
            map.put("plant", plantMap);
        }
        return map;
    }

    public static List<Map<String, Object>> toOrderItems(List<OrderItem> items, PlantLookup lookup) throws Exception {
        List<Map<String, Object>> mapped = new ArrayList<>(items.size());
        for (OrderItem item : items) {
            Plant plant = lookup.find(item.plantId);
            if (plant == null) {
                continue;
            }
            mapped.add(toOrderItem(item, plant));
        }
        return mapped;
    }

    private static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }

    private static Map<String, Object> base() {
        return new LinkedHashMap<>();
    }

    @FunctionalInterface
    public interface PlantLookup {
        Plant find(int id) throws Exception;
    }
}