package model;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Modèle représentant une commande.
 * Contient les informations de la commande : utilisateur, total, statut.
 */
public final class Order {
    public int id;
    public int userId;
    public BigDecimal total;
    public String status;
    public Timestamp createdAt;

    /**
     * Constructeur complet pour une commande existante.
     * @param id Identifiant unique
     * @param userId Identifiant de l'utilisateur
     * @param total Montant total
     * @param status Statut de la commande
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
     * Constructeur pour la création d'une nouvelle commande.
     * @param userId Identifiant de l'utilisateur
     * @param total Montant total
     * @param status Statut initial
     */
    public Order(int userId, BigDecimal total, String status) {
        this(0, userId, total, status, null);
    }
    /**
     * Constructeur par défaut pour la désérialisation JSON.
     */
    public Order() {}
}
