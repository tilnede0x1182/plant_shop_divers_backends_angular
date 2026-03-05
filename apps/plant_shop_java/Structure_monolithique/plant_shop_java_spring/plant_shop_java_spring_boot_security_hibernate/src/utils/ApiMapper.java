package utils;

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

    /**
	 * Constructeur privé pour empêcher l'instanciation.
	 */
	private ApiMapper() {}

    /**
	 * Convertit un utilisateur en Map pour la sérialisation JSON.
	 * @param user L'utilisateur à convertir
	 * @return La Map représentant l'utilisateur
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
	 * Convertit une plante en Map pour la sérialisation JSON.
	 * @param plant La plante à convertir
	 * @return La Map représentant la plante
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
	 * Convertit une commande en Map pour la sérialisation JSON.
	 * @param order La commande à convertir
	 * @param items Les articles de la commande
	 * @return La Map représentant la commande
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
	 * Convertit un article de commande en Map.
	 * @param item L'article à convertir
	 * @param plant La plante associée
	 * @return La Map représentant l'article
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
	 * Convertit une liste d'articles en liste de Maps.
	 * @param items Les articles à convertir
	 * @param lookup Fonction pour trouver les plantes
	 * @return La liste de Maps
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
	 * @return Le Double ou null
	 */
	private static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
	 * Convertit un Timestamp en chaîne ISO.
	 * @param timestamp Le timestamp à convertir
	 * @return La chaîne ISO ou null
	 */
	private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }

    /**
     * Interface fonctionnelle pour permettre au mapper de
     * trouver une plante par son ID lors de la construction
     * de la réponse d'une commande.
     */
    @FunctionalInterface
    public interface PlantLookup {
        /**
		 * Trouve une plante par son identifiant.
		 * @param id L'identifiant de la plante
		 * @return La plante trouvée
		 */
		Plant find(int id) throws Exception;
    }
}
