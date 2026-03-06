package models;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modele representant une plante.
 */
public final class Plant {
    public int id;
    public String name;
    public String description; // nullable
    public BigDecimal price;
    public int stock;
    public Timestamp createdAt; // null lors de l’insertion

    /**
     * Constructeur par defaut requis pour la deserialisation JSON.
     */
    public Plant() {
        // Constructeur par défaut requis pour la désérialisation JSON (JAX-RS / Jackson)
    }

    /**
     * Constructeur complet.
     *
     * @param id ID de la plante
     * @param name Nom de la plante
     * @param description Description (nullable)
     * @param price Prix
     * @param stock Stock disponible
     * @param createdAt Date de creation
     */
    public Plant(int id, String name, String description, BigDecimal price, int stock, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    /**
     * Constructeur simplifie pour la creation.
     *
     * @param name Nom de la plante
     * @param description Description
     * @param price Prix
     * @param stock Stock initial
     */
    public Plant(String name, String description, BigDecimal price, int stock) {
        this(0, name, description, price, stock, null);
    }
}
