package models;

import java.math.BigDecimal;
import java.sql.Timestamp;

public final class Plant {
    public int id;
    public String name;
    public String description; // nullable
    public BigDecimal price;
    public int stock;
    public Timestamp createdAt; // null lors de l’insertion

    public Plant() {
        // Constructeur par défaut requis pour la désérialisation JSON (JAX-RS / Jackson)
    }

    public Plant(int id, String name, String description, BigDecimal price, int stock, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    public Plant(String name, String description, BigDecimal price, int stock) {
        this(0, name, description, price, stock, null);
    }
}
