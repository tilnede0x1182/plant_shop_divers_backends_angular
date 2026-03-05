package models;
import java.math.BigDecimal;

/**
 * Modèle représentant un article de commande.
 */
public final class OrderItem {
    public int id;
    public int orderId;
    public int plantId;
    public int quantity;
    public BigDecimal price;

    /**
     * Constructeur complet.
     *
     * @param id int Identifiant de l'article
     * @param orderId int Identifiant de la commande
     * @param plantId int Identifiant de la plante
     * @param quantity int Quantité commandée
     * @param price BigDecimal Prix unitaire
     */
    public OrderItem(int id, int orderId, int plantId, int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.plantId = plantId;
        this.quantity = quantity;
        this.price = price;
    }
    /**
     * Constructeur pour création.
     *
     * @param orderId int Identifiant de la commande
     * @param plantId int Identifiant de la plante
     * @param quantity int Quantité commandée
     * @param price BigDecimal Prix unitaire
     */
    public OrderItem(int orderId, int plantId, int quantity, BigDecimal price) {
        this(0, orderId, plantId, quantity, price);
    }
    public OrderItem() {} // Nécessaire pour la désérialisation JSON
}
