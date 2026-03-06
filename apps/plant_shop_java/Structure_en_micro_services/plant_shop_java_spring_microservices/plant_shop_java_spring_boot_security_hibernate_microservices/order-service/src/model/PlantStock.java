package model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Entité JPA pour le stock des plantes.
 */
@Entity
@Table(name = "plants")
public class PlantStock {
    @Id
    public int id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public BigDecimal price;

    public int stock;

    /**
     * Constructeur complet.
     * @param id int Identifiant
     * @param name String Nom
     * @param price BigDecimal Prix
     * @param stock int Stock
     */
    public PlantStock(int id, String name, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    /** Constructeur par défaut pour JPA. */
    public PlantStock() {}
}
