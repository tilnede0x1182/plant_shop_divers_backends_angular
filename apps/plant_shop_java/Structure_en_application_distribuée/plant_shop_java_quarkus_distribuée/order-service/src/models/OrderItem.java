package models;

import java.math.BigDecimal;

public final class OrderItem {
    public int id;
    public int orderId;
    public int plantId;
    public int quantity;
    public BigDecimal price;

    public OrderItem(int id, int orderId, int plantId, int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.plantId = plantId;
        this.quantity = quantity;
        this.price = price;
    }

    public OrderItem(int orderId, int plantId, int quantity, BigDecimal price) {
        this(0, orderId, plantId, quantity, price);
    }

    // Constructeur par défaut nécessaire pour la désérialisation JSON
    public OrderItem() {}
}
