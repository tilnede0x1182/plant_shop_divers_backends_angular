package repositories;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.sql.*;
import java.util.*;
import models.OrderItem;

/**
 * Repository pour les articles de commande.
 * Gere les operations CRUD sur la table order_items.
 */
@Dependent
public class OrderItemRepository extends BaseRepository<OrderItem> {

    /**
     * Constructeur avec injection de la connexion.
     *
     * @param db Connexion a la base de donnees
     */
    @Inject
    public OrderItemRepository(Connection db) {
        super(db, "order_items");
    }

    /**
     * Mappe un ResultSet vers un objet OrderItem.
     *
     * @param rs ResultSet positionne sur une ligne
     * @return Objet OrderItem
     * @throws SQLException En cas d'erreur de lecture
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
     * Cree un nouvel article de commande.
     *
     * @param it Article a creer
     * @return ID de l'article cree
     * @throws SQLException En cas d'erreur d'insertion
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
     *
     * @param orderId ID de la commande
     * @return Liste des articles
     * @throws SQLException En cas d'erreur de lecture
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
     *
     * @param orderId ID de la commande
     * @throws SQLException En cas d'erreur de suppression
     */
    public void deleteByOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}
