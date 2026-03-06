package models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Entité JPA représentant une plante du catalogue.
 */
@Entity
@Table(name = "plants")
public class Plant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(nullable = false)
    public String name;

    public String description;

    @Column(nullable = false)
    public BigDecimal price;

    public int stock;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public Timestamp createdAt;

    /**
	 * Constructeur complet.
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
	 * @param name String Nom de la plante
	 * @param description String Description
	 * @param price BigDecimal Prix
	 * @param stock int Stock disponible
	 */
    public Plant(String name, String description, BigDecimal price, int stock) {
        this(0, name, description, price, stock, null);
    }
    /**
	 * Constructeur par défaut pour JPA/JSON.
	 */
    public Plant() {}
}
