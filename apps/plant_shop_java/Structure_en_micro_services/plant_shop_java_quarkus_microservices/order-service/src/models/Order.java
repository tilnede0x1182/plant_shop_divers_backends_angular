package models;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modele representant une commande.
 */
public final class Order {
    public int id;
    public int userId;
    public BigDecimal total;
    public String status;
    public Timestamp createdAt; // null lors de l’insertion

    /**
     * Constructeur complet.
     *
     * @param id ID de la commande
     * @param userId ID de l'utilisateur
     * @param total Montant total
     * @param status Statut de la commande
     * @param createdAt Date de creation
     */
    public Order(int id, int userId, BigDecimal total, String status, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Constructeur simplifie pour la creation.
     *
     * @param userId ID de l'utilisateur
     * @param total Montant total
     * @param status Statut de la commande
     */
    public Order(int userId, BigDecimal total, String status) {
        this(0, userId, total, status, null);
    }

    // Constructeur par défaut nécessaire pour la désérialisation JSON
    /**
     * Constructeur par defaut necessaire pour la deserialisation JSON.
     */
    public Order() {}
}
