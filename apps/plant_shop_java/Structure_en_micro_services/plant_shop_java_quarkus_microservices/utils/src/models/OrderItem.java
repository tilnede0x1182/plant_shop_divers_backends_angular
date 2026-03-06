package models;

import java.math.BigDecimal;

/**
 * Modele representant un article d'une commande.
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
     * @param id ID de l'article
     * @param orderId ID de la commande
     * @param plantId ID de la plante
     * @param quantity Quantite commandee
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
     * Constructeur simplifie pour la creation.
     *
     * @param orderId ID de la commande
     * @param plantId ID de la plante
     * @param quantity Quantite commandee
     * @param price Prix unitaire
     */
    public OrderItem(int orderId, int plantId, int quantity, BigDecimal price) {
        this(0, orderId, plantId, quantity, price);
    }

    // Constructeur par défaut nécessaire pour la désérialisation JSON
    public OrderItem() {}
}
