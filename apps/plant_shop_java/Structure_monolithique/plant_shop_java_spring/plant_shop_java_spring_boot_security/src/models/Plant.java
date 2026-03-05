package models;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modèle représentant une plante du catalogue.
 */
public final class Plant {
    public int id;
    public String name;
    public String description;
    public BigDecimal price;
    public int stock;
    public Timestamp createdAt;

    /**
     * Constructeur complet.
     *
     * @param id int Identifiant de la plante
     * @param name String Nom de la plante
     * @param description String Description
     * @param price BigDecimal Prix
     * @param stock int Stock disponible
     * @param createdAt Timestamp Date de création
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
     * Constructeur pour création.
     *
     * @param name String Nom de la plante
     * @param description String Description
     * @param price BigDecimal Prix
     * @param stock int Stock disponible
     */
    public Plant(String name, String description, BigDecimal price, int stock) {
        this(0, name, description, price, stock, null);
    }
    public Plant() {} // Nécessaire pour la désérialisation JSON par Spring
}
