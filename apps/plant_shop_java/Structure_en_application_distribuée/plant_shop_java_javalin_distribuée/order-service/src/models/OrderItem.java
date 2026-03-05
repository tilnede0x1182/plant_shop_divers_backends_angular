package model;

import java.math.BigDecimal;

/**
 * Modèle représentant un élément de commande.
 */
public final class OrderItem {
    public int        id;
    public int        orderId;
    public int        plantId;
    public int        quantity;
    public BigDecimal price;

    /**
     * Constructeur complet.
     * @param id int Identifiant
     * @param orderId int ID de la commande
     * @param plantId int ID de la plante
     * @param quantity int Quantité
     * @param price BigDecimal Prix unitaire
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
     * Constructeur pour nouvel élément.
     * @param orderId int ID de la commande
     * @param plantId int ID de la plante
     * @param quantity int Quantité
     * @param price BigDecimal Prix unitaire
     */
    public OrderItem(int orderId, int plantId, int quantity, BigDecimal price) {
        this(0, orderId, plantId, quantity, price);
    }
}
