package repository;

import io.micronaut.context.annotation.Bean;
import jakarta.inject.Singleton;
import java.sql.*;
import java.util.*;
import model.OrderItem;

/**
 * Repository pour les items de commande.
 */
@Singleton
public final class OrderItemRepository extends BaseRepository<OrderItem> {

    /**
     * Constructeur.
     * @param db Connection Connexion DB
     */
    public OrderItemRepository(Connection db) {
        super(db, "order_items");
    }

    /**
     * Mappe un ResultSet vers un OrderItem.
     * @param rs ResultSet Résultat SQL
     * @return OrderItem Item
     * @throws SQLException En cas d erreur SQL
     */
    @Override
    protected OrderItem mapFromResultSet(ResultSet rs) throws SQLException {
        return new OrderItem(
            rs.getInt("id"),
            rs.getInt("order_id"),
            rs.getInt("plant_id"),
            rs.getInt("quantity"),
            rs.getBigDecimal("price")
        );
    }

    /**
     * Crée un item.
     * @param it OrderItem Item
     * @return int ID généré
     * @throws SQLException En cas d erreur SQL
     */
    public int create(OrderItem it) throws SQLException {
        String sql = "INSERT INTO order_items(order_id, plant_id, quantity, price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, it.orderId);
            ps.setInt(2, it.plantId);
            ps.setInt(3, it.quantity);
            ps.setBigDecimal(4, it.price);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Liste les items d une commande.
     * @param orderId int ID commande
     * @return List Liste d items
     * @throws SQLException En cas d erreur SQL
     */
    public List<OrderItem> listByOrder(int orderId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(mapFromResultSet(rs));
                }
            }
        }
        return out;
    }

    /**
     * Supprime les items d une commande.
     * @param orderId int ID commande
     * @throws SQLException En cas d erreur SQL
     */
    public void deleteByOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}
