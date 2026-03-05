package repositories;

import models.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour les articles de commande.
 */
@Repository
@RequestScope
public class OrderItemRepository extends BaseRepository<OrderItem> {

    /** Constructeur avec injection. */
    @Autowired
    public OrderItemRepository(Connection db) {
        super(db, "order_items");
    }

    /** {@inheritDoc} */
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
     * Crée un nouvel article de commande.
     *
     * @param it OrderItem L'article à créer
     * @return int L'ID généré
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

    /** Liste les articles d'une commande. */
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
     * Supprime tous les articles d'une commande.
     *
     * @param orderId int Identifiant de la commande
     */
    public void deleteByOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}
