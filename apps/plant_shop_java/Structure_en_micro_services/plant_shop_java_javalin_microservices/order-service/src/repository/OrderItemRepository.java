package repository;

import java.sql.*;
import java.util.*;
import model.OrderItem;

/**
 * Repository pour la gestion des items de commande en base.
 */
public final class OrderItemRepository {

    private final Connection db;

    /**
     * Construit le repository avec la connexion BDD.
     * @param db Connexion à la base de données
     */
    public OrderItemRepository(Connection db) {
        this.db = db;
    }

    /**
     * Convertit un ResultSet en objet OrderItem.
     * @param rs ResultSet positionné sur une ligne
     * @return Objet OrderItem correspondant
     * @throws SQLException En cas d'erreur de lecture
     */
    private OrderItem mapFromResultSet(ResultSet rs) throws SQLException {
        return new OrderItem(
            rs.getInt("id"),
            rs.getInt("order_id"),
            rs.getInt("plant_id"),
            rs.getInt("quantity"),
            rs.getBigDecimal("price")
        );
    }

    /**
     * Insère un nouvel item de commande en base.
     * @param it Item à insérer
     * @return ID généré de l'item
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
}
