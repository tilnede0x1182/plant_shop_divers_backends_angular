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
import repository.PlantRepository;

/**
 * Classe utilitaire pour mapper les entités du domaine vers des Maps JSON-friendly.
 * Convertit les objets User, Plant, Order, OrderItem en structures Map.
 */
public final class ApiMapper {

    /**
     * Constructeur privé pour empêcher l'instanciation.
     */
    private ApiMapper() {}

    /**
     * Convertit un utilisateur en Map pour sérialisation JSON.
     * @param user L'utilisateur à convertir
     * @return Map contenant les données de l'utilisateur
     */
    public static Map<String, Object> toUser(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.id);
        map.put("name", user.name);
        map.put("email", user.email);
        map.put("admin", user.isAdmin);
        map.put("createdAt", toIso(user.createdAt));
        return map;
    }

    /**
     * Convertit une plante en Map pour sérialisation JSON.
     * @param plant La plante à convertir
     * @return Map contenant les données de la plante
     */
    public static Map<String, Object> toPlant(Plant plant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", plant.id);
        map.put("name", plant.name);
        map.put("description", plant.description);
        map.put("price", toDecimal(plant.price));
        map.put("stock", plant.stock);
        map.put("createdAt", toIso(plant.createdAt));
        return map;
    }

    /**
     * Convertit une commande en Map pour sérialisation JSON.
     * @param order La commande à convertir
     * @param items Les articles de la commande déjà convertis
     * @return Map contenant les données de la commande
     */
    public static Map<String, Object> toOrder(Order order, List<Map<String, Object>> items) {
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
     * Convertit un article de commande en Map pour sérialisation JSON.
     * @param item L'article à convertir
     * @param plant La plante associée (peut être null)
     * @return Map contenant les données de l'article
     */
    public static Map<String, Object> toOrderItem(OrderItem item, Plant plant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", item.id);
        map.put("orderId", item.orderId);
        map.put("plantId", item.plantId);
        map.put("quantity", item.quantity);
        map.put("price", toDecimal(item.price));
        if (plant != null) {
            map.put("plant", toPlant(plant));
        }
        return map;
    }

    /**
     * Convertit une liste d'articles de commande en liste de Maps.
     * @param items Les articles à convertir
     * @param lookup Fonction pour récupérer les plantes par ID
     * @return Liste de Maps contenant les données des articles
     * @throws Exception Si la recherche de plante échoue
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
     * @param value La valeur à convertir
     * @return Double ou null si valeur nulle
     */
    private static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * Convertit un Timestamp en chaîne ISO 8601.
     * @param timestamp Le timestamp à convertir
     * @return Chaîne ISO ou null si timestamp nul
     */
    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }

    /**
     * Interface fonctionnelle pour la recherche de plantes.
     */
    @FunctionalInterface
    public interface PlantLookup {
        Plant find(int id) throws Exception;
    }
}
