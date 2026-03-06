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

/**
 * Utilitaire de conversion modèles vers format API.
 */
public final class ApiMapper {

    /**
 * Constructeur privé - classe utilitaire.
 */
private ApiMapper() {
        // utilitaire statique
    }

    /**
	 * Convertit une commande en Map API.
	 * @param order Commande à convertir
	 * @param items Liste des items
	 * @return Map pour API
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
	 * Convertit un élément de commande en Map API.
	 * @param item Élément à convertir
	 * @param plant Plante associée
	 * @return Map pour API
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
	 * Convertit une liste d'éléments de commande.
	 * @param items Éléments à convertir
	 * @param lookup Fonction de recherche de plante
	 * @return Liste de Maps pour API
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
 * @param value Valeur à convertir
 * @return Double ou null
 */
private static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
	 * Convertit un timestamp en chaîne ISO.
	 * @param timestamp Timestamp à convertir
	 * @return Chaîne ISO ou null
	 */
	private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }

    /**
	 * Crée une Map de base ordonnée.
	 * @return LinkedHashMap vide
	 */
	private static Map<String, Object> base() {
        return new LinkedHashMap<>();
    }

    /**
	 * Interface fonctionnelle pour rechercher une plante.
	 */
	@FunctionalInterface
    public interface PlantLookup {
        /**
		 * Trouve une plante par ID.
		 * @param id Identifiant de la plante
		 * @return Plante trouvée
		 */
		Plant find(int id) throws Exception;
    }
}