import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour les articles de commande.
 */
public final class OrderItemRepository extends OrderBaseRepository<OrderItem> {

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	public OrderItemRepository(Connection db) {
        super(db, "order_items");
    }

    /**
	 * Mappe un ResultSet vers un OrderItem.
	 * @param rs Le ResultSet
	 * @return L'article mappé
	 */
	@Override
    OrderItem map(ResultSet rs) throws SQLException {
        return new OrderItem(
            rs.getInt("id"),
            rs.getInt("order_id"),
            rs.getInt("plant_id"),
            rs.getInt("quantity"),
            rs.getBigDecimal("price")
        );
    }

    /**
	 * Crée un nouvel article.
	 * @param item L'article à créer
	 * @return L'identifiant généré
	 */
	public int create(OrderItem item) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO order_items(order_id, plant_id, quantity, price) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.orderId);
            ps.setInt(2, item.plantId);
            ps.setInt(3, item.quantity);
            ps.setBigDecimal(4, item.price);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
	 * Liste les articles d'une commande.
	 * @param orderId L'identifiant de la commande
	 * @return La liste des articles
	 */
	public List<OrderItem> listByOrder(int orderId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM order_items WHERE order_id=?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    /**
	 * Supprime les articles d'une commande.
	 * @param orderId L'identifiant de la commande
	 */
	public void deleteByOrder(int orderId) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM order_items WHERE order_id=?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}

/**
 * Repository local pour les plantes.
 */
final class PlantRepository {
    private final Connection db;

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	PlantRepository(Connection db) {
        this.db = db;
    }

    /**
	 * Trouve une plante par son identifiant.
	 * @param id L'identifiant
	 * @return La plante ou null
	 */
	Plant find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM plants WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Plant(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock")
                    );
                }
            }
        }
        return null;
    }

    /**
	 * Met à jour le stock d'une plante.
	 * @param id L'identifiant de la plante
	 * @param stock Le nouveau stock
	 */
	void updateStock(int id, int stock) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("UPDATE plants SET stock=? WHERE id=?")) {
            ps.setInt(1, stock);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
