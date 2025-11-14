package repository;

import java.sql.*;
import java.util.*;
import model.OrderItem;
import order.repositories.BaseRepository;

public final class OrderItemRepository extends BaseRepository<OrderItem> {

    public OrderItemRepository(Connection db) {
        super(db, "order_items");
    }

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

    // La méthode `create` remplace `addItem` pour la cohérence
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
     * Récupère tous les items associés à une commande spécifique.
     * @param orderId L'ID de la commande.
     * @return Une liste d'objets OrderItem.
     * @throws SQLException
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
     * Supprime tous les items associés à une commande spécifique.
     * @param orderId L'ID de la commande.
     * @throws SQLException
     */
    public void deleteByOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }

    // Les méthodes `find(id)`, `list()` (pour tous les items de toutes les commandes)
    // et `delete(id)` (pour un item spécifique) sont maintenant héritées de BaseRepository.
}
