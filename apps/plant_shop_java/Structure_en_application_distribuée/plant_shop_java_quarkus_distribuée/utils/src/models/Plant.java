package models;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modèle représentant une plante.
 */
public final class Plant {
    public int id;
    public String name;
    public String description; // nullable
    public BigDecimal price;
    public int stock;
    public Timestamp createdAt; // null lors de l’insertion

    /**
     * Constructeur par défaut requis pour la désérialisation JSON.
     */
    public Plant() {
        // Constructeur par défaut requis pour la désérialisation JSON (JAX-RS / Jackson)
    }

    /**
     * Constructeur complet (lecture DB).
     * @param id ID de la plante
     * @param name Nom
     * @param description Description
     * @param price Prix
     * @param stock Stock
     * @param createdAt Date de création
     */
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
