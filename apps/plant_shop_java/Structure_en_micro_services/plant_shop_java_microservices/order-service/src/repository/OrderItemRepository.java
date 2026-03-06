package order.repository;

import order.model.OrderItem;
import order.model.PlantStock;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour les items de commande.
 */
public final class OrderItemRepository {

    private final Connection db;

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	public OrderItemRepository(Connection db) {
        this.db = db;
    }

    /**
	 * Mappe un ResultSet vers un OrderItem.
	 * @param rs ResultSet
	 * @return OrderItem
	 * @throws SQLException En cas d'erreur SQL
	 */
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
	 * Crée un item de commande.
	 * @param item Item à créer
	 * @return ID généré
	 * @throws SQLException En cas d'erreur SQL
	 */
	public int create(OrderItem item) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO order_items(order_id, plant_id, quantity, price) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.orderId());
            ps.setInt(2, item.plantId());
            ps.setInt(3, item.quantity());
            ps.setBigDecimal(4, item.price());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
	 * Trouve les items d'une commande.
	 * @param orderId ID de la commande
	 * @return Liste des items
	 * @throws SQLException En cas d'erreur SQL
	 */
	public List<OrderItem> findByOrder(int orderId) throws SQLException {
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
	 * Supprime les items d'une commande.
	 * @param orderId ID de la commande
	 * @throws SQLException En cas d'erreur SQL
	 */
	public void deleteByOrder(int orderId) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM order_items WHERE order_id=?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}
