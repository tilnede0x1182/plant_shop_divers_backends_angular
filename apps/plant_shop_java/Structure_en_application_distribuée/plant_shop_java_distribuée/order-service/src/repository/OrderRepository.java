import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository de base pour le service de commandes.
 * @param <T> Type de l'entité
 */
abstract class OrderBaseRepository<T> {
    protected final Connection db;
    protected final String table;

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 * @param table Nom de la table
	 */
	OrderBaseRepository(Connection db, String table) {
        this.db = db;
        this.table = table;
    }

    /**
	 * Mappe un ResultSet vers une entité.
	 * @param rs Le ResultSet
	 * @return L'entité mappée
	 */
	abstract T map(ResultSet rs) throws SQLException;

    /**
	 * Trouve une entité par son identifiant.
	 * @param id L'identifiant
	 * @return L'entité ou null
	 */
	T find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    /**
	 * Supprime une entité.
	 * @param id L'identifiant
	 */
	void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

/**
 * Repository pour les commandes.
 */
public final class OrderRepository extends OrderBaseRepository<Order> {

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	public OrderRepository(Connection db) {
        super(db, "orders");
    }

    /**
	 * Mappe un ResultSet vers un objet Order.
	 * @param rs Le ResultSet
	 * @return La commande mappée
	 */
	@Override
    Order map(ResultSet rs) throws SQLException {
        return new Order(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    /**
	 * Crée une nouvelle commande.
	 * @param order La commande à créer
	 * @return L'identifiant généré
	 */
	public int create(Order order) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO orders(user_id, total, status) VALUES (?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.userId);
            ps.setBigDecimal(2, order.total);
            ps.setString(3, order.status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
	 * Met à jour le total d'une commande.
	 * @param id L'identifiant de la commande
	 * @param total Le nouveau total
	 */
	public void updateTotal(int id, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("UPDATE orders SET total=? WHERE id=?")) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
	 * Met à jour le statut d'une commande.
	 * @param id L'identifiant de la commande
	 * @param status Le nouveau statut
	 */
	public void updateStatus(int id, String status) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("UPDATE orders SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
	 * Liste les commandes d'un utilisateur.
	 * @param userId L'identifiant de l'utilisateur
	 * @return La liste des commandes
	 */
	public List<Order> listByUser(int userId) throws SQLException {
        List<Order> out = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM orders WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }
}
