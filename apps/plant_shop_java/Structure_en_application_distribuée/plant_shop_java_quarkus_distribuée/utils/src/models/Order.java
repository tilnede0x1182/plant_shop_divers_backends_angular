package models;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modèle représentant une commande.
 */
public final class Order {
    public int id;
    public int userId;
    public BigDecimal total;
    public String status;
    public Timestamp createdAt; // null lors de l’insertion

    /**
     * Constructeur complet (lecture DB).
     * @param id ID de la commande
     * @param userId ID de l'utilisateur
     * @param total Total
     * @param status Statut
     * @param createdAt Date de création
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
     * @param userId ID de l'utilisateur
     * @param total Total
     * @param status Statut
     */
    public Order(int userId, BigDecimal total, String status) {
        this(0, userId, total, status, null);
    }

    // Constructeur par défaut nécessaire pour la désérialisation JSON
    public Order() {}
}
