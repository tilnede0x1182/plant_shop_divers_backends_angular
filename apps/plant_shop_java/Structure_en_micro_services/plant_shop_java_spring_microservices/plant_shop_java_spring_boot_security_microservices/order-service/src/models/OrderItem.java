package model;
import java.math.BigDecimal;

/**
 * Modèle représentant un item de commande.
 * Lie une plante à une commande avec une quantité et un prix.
 */
public final class OrderItem {
    public int id;
    public int orderId;
    public int plantId;
    public int quantity;
    public BigDecimal price;

    /**
     * Constructeur complet pour un item existant.
     * @param id Identifiant unique
     * @param orderId Identifiant de la commande
     * @param plantId Identifiant de la plante
     * @param quantity Quantité commandée
     * @param price Prix unitaire
     */
    public OrderItem(int id, int orderId, int plantId, int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.plantId = plantId;
        this.quantity = quantity;
        this.price = price;
    }
    /**
     * Constructeur pour la création d'un nouvel item.
     * @param orderId Identifiant de la commande
     * @param plantId Identifiant de la plante
     * @param quantity Quantité commandée
     * @param price Prix unitaire
     */
    public OrderItem(int orderId, int plantId, int quantity, BigDecimal price) {
        this(0, orderId, plantId, quantity, price);
    }
    /**
     * Constructeur par défaut pour la désérialisation JSON.
     */
    public OrderItem() {}
}
