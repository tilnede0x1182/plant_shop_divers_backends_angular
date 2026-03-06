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
     * Constructeur complet avec tous les champs.
     *
     * @param id int Identifiant de la commande
     * @param userId int Identifiant de l'utilisateur
     * @param total BigDecimal Montant total
     * @param status String Statut de la commande
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
     * Constructeur pour création (sans id ni date).
     *
     * @param userId int Identifiant de l'utilisateur
     * @param total BigDecimal Montant total
     * @param status String Statut de la commande
     */
    public Order(int userId, BigDecimal total, String status) {
        this(0, userId, total, status, null);
    }

    /** Constructeur par défaut nécessaire pour la désérialisation JSON. */
    public Order() {}
}
