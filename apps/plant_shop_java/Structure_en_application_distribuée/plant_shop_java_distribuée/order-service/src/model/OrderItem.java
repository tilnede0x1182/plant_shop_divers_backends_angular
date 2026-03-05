import java.math.BigDecimal;

/**
 * Modèle représentant un article de commande.
 */
public final class OrderItem {
    int id;
    int orderId;
    int plantId;
    int quantity;
    BigDecimal price;

    /**
	 * Constructeur complet.
	 * @param id Identifiant
	 * @param orderId Identifiant de la commande
	 * @param plantId Identifiant de la plante
	 * @param quantity Quantité
	 * @param price Prix unitaire
	 */
	public OrderItem(int id, int orderId, int plantId, int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.plantId = plantId;
        this.quantity = quantity;
        this.price = price;
    }
}

/**
 * Modèle local de plante pour le service de commandes.
 */
final class Plant {
    int id;
    String name;
    BigDecimal price;
    int stock;

    /**
	 * Constructeur.
	 * @param id Identifiant
	 * @param name Nom
	 * @param price Prix
	 * @param stock Stock disponible
	 */
	Plant(int id, String name, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}
