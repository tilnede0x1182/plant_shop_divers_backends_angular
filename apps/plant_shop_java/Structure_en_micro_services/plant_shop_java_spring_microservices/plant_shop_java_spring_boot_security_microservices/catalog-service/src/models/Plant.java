package model;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modèle représentant une plante du catalogue.
 * Contient les informations de produit : nom, description, prix, stock.
 */
public final class Plant {
    public int id;
    public String name;
    public String description;
    public BigDecimal price;
    public int stock;
    public Timestamp createdAt;

    /**
     * Constructeur complet pour une plante existante.
     * @param id Identifiant unique
     * @param name Nom de la plante
     * @param description Description textuelle
     * @param price Prix unitaire
     * @param stock Quantité en stock
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
    /**
     * Constructeur pour la création d'une nouvelle plante.
     * @param name Nom de la plante
     * @param description Description textuelle
     * @param price Prix unitaire
     * @param stock Quantité en stock
     */
    public Plant(String name, String description, BigDecimal price, int stock) {
        this(0, name, description, price, stock, null);
    }
    /**
     * Constructeur par défaut pour la désérialisation JSON.
     */
    public Plant() {}
}
