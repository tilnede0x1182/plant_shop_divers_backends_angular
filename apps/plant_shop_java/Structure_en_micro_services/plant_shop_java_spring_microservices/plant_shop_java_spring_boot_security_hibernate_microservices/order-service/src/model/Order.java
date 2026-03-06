package model;

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
 * Entité JPA représentant une commande.
 */
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(name = "user_id", nullable = false)
    public int userId;

    @Column(name = "total", nullable = false)
    public BigDecimal total;

    @Column(nullable = false)
    public String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public Timestamp createdAt;

    /**
     * Constructeur complet.
     * @param id int Identifiant
     * @param userId int ID utilisateur
     * @param total BigDecimal Total
     * @param status String Statut
     * @param createdAt Timestamp Date de création
     */
    public Order(int id, int userId, BigDecimal total, String status, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }
    /**
     * Constructeur pour création.
     * @param userId int ID utilisateur
     * @param total BigDecimal Total
     * @param status String Statut
     */
    public Order(int userId, BigDecimal total, String status) {
        this(0, userId, total, status, null);
    }
    /** Constructeur par défaut pour JPA. */
    public Order() {}
}
