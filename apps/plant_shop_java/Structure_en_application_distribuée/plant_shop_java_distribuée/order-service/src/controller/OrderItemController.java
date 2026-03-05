import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.List;

/**
 * Contrôleur pour la conversion des articles de commande en JSON.
 */
final class OrderItemController {

    private final PlantRepository plantRepo;

    /**
	 * Constructeur avec repository de plantes.
	 * @param plantRepo Repository de plantes
	 */
	OrderItemController(PlantRepository plantRepo) {
        this.plantRepo = plantRepo;
    }

    /**
	 * Convertit une commande et ses articles en JSON.
	 * @param order La commande
	 * @param items Les articles de la commande
	 * @return L'objet JSON
	 */
	JSONObject toJson(Order order, List<OrderItem> items) throws SQLException {
        JSONArray itemsJson = new JSONArray();
        for (OrderItem item : items) {
            JSONObject obj = new JSONObject()
                .put("id", item.id)
                .put("orderId", item.orderId)
                .put("plantId", item.plantId)
                .put("quantity", item.quantity)
                .put("price", item.price);
            Plant plant = plantRepo.find(item.plantId);
            if (plant != null) {
                obj.put("plant", new JSONObject()
                    .put("id", plant.id)
                    .put("name", plant.name)
                    .put("price", plant.price));
            }
            itemsJson.put(obj);
        }

        return new JSONObject()
            .put("id", order.id)
            .put("userId", order.userId)
            .put("totalPrice", order.total)
            .put("status", order.status)
            .put("createdAt", order.createdAt.toString())
            .put("orderItems", itemsJson);
    }

}
