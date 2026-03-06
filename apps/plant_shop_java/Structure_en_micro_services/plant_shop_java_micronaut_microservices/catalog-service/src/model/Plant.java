package model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Entité représentant une plante.
 */
@Introspected
@Serdeable
public final class Plant {
    public int id;
    public String name;
    public String description; // nullable
    public BigDecimal price;
    public int stock;
    public Timestamp createdAt; // null lors de l’insertion

    /**
     * Constructeur complet avec tous les champs.
     * @param id Identifiant de la plante
     * @param name Nom de la plante
     * @param description Description
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
     * Constructeur simplifié pour création.
     * @param name Nom de la plante
     * @param description Description
     * @param price Prix unitaire
     * @param stock Quantité en stock
     */
    public Plant(String name, String description, BigDecimal price, int stock) {
        this(0, name, description, price, stock, null);
    }
}
