package order.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Représente une commande.
 * @param id ID de la commande
 * @param userId ID de l'utilisateur
 * @param total Montant total
 * @param status Statut de la commande
 * @param createdAt Date de création
 */
public record Order(
    int id,
    int userId,
    BigDecimal total,
    String status,
    Instant createdAt
) {
    /**
	 * Constructeur simplifié avec valeurs par défaut.
	 * @param id ID de la commande
	 * @param userId ID de l'utilisateur
	 * @param total Montant total
	 */
	public Order(int id, int userId, BigDecimal total) {
        this(id, userId, total, "pending", Instant.now());
    }

    /**
	 * Crée une copie avec un nouveau total.
	 * @param value Nouveau total
	 * @return Nouvelle instance
	 */
	public Order withTotal(BigDecimal value) {
        return new Order(id, userId, value, status, createdAt);
    }

    /**
	 * Crée une copie avec un nouveau statut.
	 * @param value Nouveau statut
	 * @return Nouvelle instance
	 */
	public Order withStatus(String value) {
        return new Order(id, userId, total, value, createdAt);
    }
}
