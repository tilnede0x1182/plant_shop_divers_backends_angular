package model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modèle représentant une commande.
 */
@Introspected
@Serdeable
public final class Order {
    public int id;
    public int userId;
    public BigDecimal total;
    public String status;
    public Timestamp createdAt; // null lors de l’insertion

    /**
     * Constructeur complet.
     * @param id int Identifiant
     * @param userId int ID utilisateur
     * @param total BigDecimal Total
     * @param status String Statut
     * @param createdAt Timestamp Date création
     */
    public Order(int id, int userId, BigDecimal total, String status, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Constructeur pour insertion.
     * @param userId int ID utilisateur
     * @param total BigDecimal Total
     * @param status String Statut
     */
    public Order(int userId, BigDecimal total, String status) {
        this(0, userId, total, status, null);
    }
}
