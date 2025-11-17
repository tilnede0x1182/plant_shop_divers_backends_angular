package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(name = "order_id", nullable = false)
    public int orderId;

    @Column(name = "plant_id", nullable = false)
    public Integer plantId;

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false)
    public BigDecimal price;

    public OrderItem(int id, int orderId, Integer plantId, int quantity, BigDecimal price) {
        this.id = id;
        this.orderId = orderId;
        this.plantId = plantId;
        this.quantity = quantity;
        this.price = price;
    }
    public OrderItem(int orderId, Integer plantId, int quantity, BigDecimal price) {
        this(0, orderId, plantId, quantity, price);
    }
    public OrderItem() {} // Nécessaire pour la désérialisation JSON
}
