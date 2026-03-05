package repositories;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.sql.*;
import java.util.*;
import models.OrderItem;

/**
 * Repository pour les articles de commande.
 */
@Dependent
public class OrderItemRepository extends BaseRepository<OrderItem> {

    /**
     * Constructeur avec injection de la connexion.
     * @param db Connexion à la base de données
     */
    @Inject
    public OrderItemRepository(Connection db) {
        super(db, "order_items");
    }

    /**
     * {@inheritDoc}
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
     * Crée un nouvel article de commande.
     * @param it Article à créer
     * @return ID de l'article créé
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
     * Liste les articles d'une commande.
     * @param orderId ID de la commande
     * @return Liste des articles
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
     * Supprime tous les articles d'une commande.
     * @param orderId ID de la commande
     */
    public void deleteByOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}
