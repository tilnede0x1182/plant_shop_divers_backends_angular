package model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;

/**
 * Modèle représentant un item de commande.
 */
@Introspected
@Serdeable
public final class OrderItem {
    public int id;
    public int orderId;
    public int plantId;
    public int quantity;
    public BigDecimal price;

    /**
     * Constructeur complet.
     * @param id int Identifiant
     * @param orderId int ID commande
     * @param plantId int ID plante
     * @param quantity int Quantité
     * @param price BigDecimal Prix
     */
    public OrderItem(int id, int orderId, int plantId, int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.plantId = plantId;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * Constructeur pour insertion.
     * @param orderId int ID commande
     * @param plantId int ID plante
     * @param quantity int Quantité
     * @param price BigDecimal Prix
     */
    public OrderItem(int orderId, int plantId, int quantity, BigDecimal price) {
        this(0, orderId, plantId, quantity, price);
    }
}
