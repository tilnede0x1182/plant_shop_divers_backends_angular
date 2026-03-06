package util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import models.Order;
import models.OrderItem;
import models.Plant;
import models.User;
/**
 * Classe utilitaire pour mapper les objets Modèle en Map<String, Object>
 * prêtes à être sérialisées en JSON par Jackson.
 */
public final class ApiMapper {

    /** Constructeur prive pour empecher l'instanciation. */
    private ApiMapper() {}

    /**
     * Convertit un User en Map pour serialisation JSON.
     *
     * @param user Utilisateur a convertir
     * @return Map representant l'utilisateur
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
     * Convertit une Plant en Map pour serialisation JSON.
     *
     * @param plant Plante a convertir
     * @return Map representant la plante
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
     * Convertit une Order en Map pour serialisation JSON.
     *
     * @param order Commande a convertir
     * @param items Liste des articles deja convertis
     * @return Map representant la commande
     */
    public static Map<String, Object> toOrder(Order order, List<Map<String, Object>> items) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", order.id);
        map.put("userId", order.userId);
        // Le test attend "totalPrice", pas "total"
        map.put("totalPrice", toDecimal(order.total));
        map.put("status", order.status);
        map.put("createdAt", toIso(order.createdAt));
        map.put("orderItems", items);
        return map;
    }

    /**
     * Convertit un OrderItem en Map pour serialisation JSON.
     *
     * @param item Article a convertir
     * @param plant Plante associee
     * @return Map representant l'article
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
     * Convertit une liste d'OrderItem en liste de Maps.
     *
     * @param items Liste des articles
     * @param lookup Service de recherche de plantes
     * @return Liste de Maps representant les articles
     * @throws Exception En cas d'erreur de recherche
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
     *
     * @param value Valeur a convertir
     * @return Double ou null
     */
    private static Double toDecimal(BigDecimal value) {
        return value == null ?
        null : value.doubleValue();
    }

    /**
     * Convertit un Timestamp en format ISO 8601.
     *
     * @param timestamp Timestamp a convertir
     * @return Chaine ISO ou null
     */
    private static String toIso(Timestamp timestamp) {
        return timestamp == null ?
        null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }

    /**
     * Interface fonctionnelle pour permettre au mapper de
     * trouver une plante par son ID lors de la construction
     * de la réponse d'une commande.
     */
    @FunctionalInterface
    public interface PlantLookup {
        Plant find(int id) throws Exception;
    }
}
