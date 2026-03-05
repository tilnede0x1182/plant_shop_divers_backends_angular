package util;

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
import model.User;

/**
 * Regroupe les transformations des entités métier vers des structures simples
 * directement sérialisables en JSON par Javalin.
 */
public final class ApiMapper {

    /**
     * Constructeur privé (utilitaire statique).
     */
    private ApiMapper() {
        // utilitaire statique
    }

    /**
     * Convertit un User en Map.
     * @param user User Utilisateur
     * @return Map Données sérialisables
     */
    public static Map<String, Object> toUser(User user) {
        Map<String, Object> map = base();
        map.put("id", user.id);
        map.put("name", user.name);
        map.put("email", user.email);
        map.put("admin", user.isAdmin);
        map.put("createdAt", toIso(user.createdAt));
        return map;
    }

    /**
     * Convertit un Plant en Map.
     * @param plant Plant Plante
     * @return Map Données sérialisables
     */
    public static Map<String, Object> toPlant(Plant plant) {
        Map<String, Object> map = base();
        map.put("id", plant.id);
        map.put("name", plant.name);
        map.put("description", plant.description);
        map.put("price", toDecimal(plant.price));
        map.put("stock", plant.stock);
        map.put("createdAt", toIso(plant.createdAt));
        return map;
    }

    /**
     * Convertit un Order en Map.
     * @param order Order Commande
     * @param items List Items de la commande
     * @return Map Données sérialisables
     */
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

    /**
     * Convertit un OrderItem en Map.
     * @param item OrderItem Item
     * @param plant Plant Plante associée
     * @return Map Données sérialisables
     */
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

    /**
     * Convertit une liste d items en liste de Maps.
     * @param items List Liste d items
     * @param lookup PlantLookup Fonction de recherche de plante
     * @return List Liste de Maps
     * @throws Exception En cas d erreur
     */
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

    /**
     * Convertit un BigDecimal en Double.
     * @param value BigDecimal Valeur
     * @return Double Valeur convertie
     */
    private static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * Convertit un Timestamp en chaîne ISO.
     * @param timestamp Timestamp Date
     * @return String Date ISO
     */
    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }

    /**
     * Crée une Map de base vide.
     * @return Map Map vide ordonnée
     */
    private static Map<String, Object> base() {
        return new LinkedHashMap<>();
    }

    /**
     * Interface fonctionnelle pour recherche de plante.
     */
    @FunctionalInterface
    public interface PlantLookup {
        /**
         * Recherche une plante par ID.
         * @param id int ID de la plante
         * @return Plant Plante trouvée
         * @throws Exception En cas d erreur
         */
        Plant find(int id) throws Exception;
    }
}
