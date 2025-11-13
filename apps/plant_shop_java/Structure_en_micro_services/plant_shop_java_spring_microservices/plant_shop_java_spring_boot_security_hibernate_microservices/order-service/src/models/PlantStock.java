package models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

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

    public PlantStock(int id, String name, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public PlantStock() {} // Nécessaire pour Hibernate
}
