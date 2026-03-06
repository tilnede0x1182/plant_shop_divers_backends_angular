package model;

import java.math.BigDecimal;

/**
 * Entité représentant un item de commande.
 */
public final class OrderItem {
    public int        id;
    public int        orderId;
    public int        plantId;
    public int        quantity;
    public BigDecimal price;

    /**
     * Constructeur complet avec tous les champs.
     * @param id Identifiant de l'item
     * @param orderId Identifiant de la commande parente
     * @param plantId Identifiant de la plante
     * @param quantity Quantité commandée
     * @param price Prix unitaire
     */
    public OrderItem(int id, int orderId, int plantId,
                     int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.plantId = plantId;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * Constructeur simplifié pour création (sans id).
     * @param orderId Identifiant de la commande parente
     * @param plantId Identifiant de la plante
     * @param quantity Quantité commandée
     * @param price Prix unitaire
     */
    public OrderItem(int orderId, int plantId, int quantity, BigDecimal price) {
        this(0, orderId, plantId, quantity, price);
    }
}
