package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modèle représentant une plante.
 */
public final class Plant {
    public int        id;
    public String     name;
    public String     description;  // nullable
    public BigDecimal price;
    public int        stock;
    public Timestamp  createdAt;    // null lors de l’insertion

    /**
     * Constructeur complet.
     * @param id int Identifiant
     * @param name String Nom
     * @param description String Description
     * @param price BigDecimal Prix
     * @param stock int Stock
     * @param createdAt Timestamp Date création
     */
    public Plant(int id, String name, String description,
                 BigDecimal price, int stock, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    /**
     * Constructeur pour insertion.
     * @param name String Nom
     * @param description String Description
     * @param price BigDecimal Prix
     * @param stock int Stock
     */
    public Plant(String name, String description, BigDecimal price, int stock) {
        this(0, name, description, price, stock, null);
    }
}
