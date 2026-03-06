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

    /**
     * Constructeur privé pour classe utilitaire.
     */
    private ApiMapper() {
        // utilitaire statique
    }

    /**
     * Convertit une commande en Map JSON.
     * @param order Commande à convertir
     * @param items Liste des items convertis
     * @return Map représentant le JSON
     */
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

    /**
     * Convertit un item de commande en Map JSON.
     * @param item Item à convertir
     * @param plant Plante associée
     * @return Map représentant le JSON
     */
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

    /**
     * Convertit un BigDecimal en Double.
     * @param value Valeur à convertir
     * @return Double ou null
     */
    private static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * Convertit un Timestamp en chaîne ISO.
     * @param timestamp Timestamp à convertir
     * @return Chaîne ISO ou null
     */
    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
}