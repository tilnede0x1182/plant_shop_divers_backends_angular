package repository;

import model.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour la gestion des items de commande en base de données.
 * Fournit les opérations CRUD sur la table order_items.
 */
@Repository
@RequestScope
public class OrderItemRepository  {

    private final Connection db;

    /**
     * Constructeur avec injection de la connexion BDD.
     * @param db Connexion à la base de données
     */
    @Autowired
    public OrderItemRepository(Connection db) {
        this.db = db;
    }

    /**
     * Mappe un ResultSet vers un objet OrderItem.
     * @param rs ResultSet positionné sur une ligne
     * @return L'item mappé
     * @throws SQLException En cas d'erreur SQL
     */
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
     * Crée un nouvel item de commande en base de données.
     * @param it Item à créer
     * @return L'identifiant généré
     * @throws SQLException En cas d'erreur SQL
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
     * Liste les items d'une commande.
     * @param orderId Identifiant de la commande
     * @return Liste des items
     * @throws SQLException En cas d'erreur SQL
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
     * Supprime tous les items d'une commande.
     * @param orderId Identifiant de la commande
     * @throws SQLException En cas d'erreur SQL
     */
    public void deleteByOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}
