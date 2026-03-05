import java.math.BigDecimal;
import java.time.Instant;

/**
 * Modèle représentant une commande.
 */
public final class Order {
    int id;
    int userId;
    BigDecimal total;
    String status;
    Instant createdAt;

    /**
	 * Constructeur complet.
	 * @param id Identifiant
	 * @param userId Identifiant de l'utilisateur
	 * @param total Montant total
	 * @param status Statut de la commande
	 * @param createdAt Date de création
	 */
	public Order(int id, int userId, BigDecimal total, String status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }
}
