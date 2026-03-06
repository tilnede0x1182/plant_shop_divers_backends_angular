package model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Introspected
@Serdeable
public final class Order {
    public int id;
    public int userId;
    public BigDecimal total;
    public String status;
    public Timestamp createdAt; // null lors de l’insertion

    /**
     * Constructeur complet avec tous les champs.
     * @param id Identifiant de la commande
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
     * Constructeur simplifié pour création.
     * @param userId Identifiant de l'utilisateur
     * @param total Montant total
     * @param status Statut de la commande
     */
    public Order(int userId, BigDecimal total, String status) {
        this(0, userId, total, status, null);
    }
}
