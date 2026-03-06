package repository;

import io.micronaut.context.annotation.Bean;
import jakarta.inject.Singleton;
import java.sql.*;
import java.util.*;
import model.OrderItem;

/**
 * Repository pour la gestion des items de commande.
 */
@Singleton
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
     * Recherche un item par son ID.
     * @param id Identifiant de l'item
     * @return Item trouvé ou null
     * @throws SQLException En cas d'erreur BDD
     */
    public OrderItem find(int id) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFromResultSet(rs);
                }
                return null;
            }
        }
    }

    /**
     * Liste tous les items.
     * @return Liste des items
     * @throws SQLException En cas d'erreur BDD
     */
    public List<OrderItem> list() throws SQLException {
        String sql = "SELECT * FROM order_items ORDER BY id";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                items.add(mapFromResultSet(rs));
            }
        }
        return items;
    }

    /**
     * Supprime un item par son ID.
     * @param id Identifiant de l'item
     * @throws SQLException En cas d'erreur BDD
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM order_items WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
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
     * Insère un nouvel item en base.
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
     * Liste les items d'une commande.
     * @param orderId Identifiant de la commande
     * @return Liste des items
     * @throws SQLException En cas d'erreur BDD
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
     * Supprime les items d'une commande.
     * @param orderId Identifiant de la commande
     * @throws SQLException En cas d'erreur BDD
     */
    public void deleteByOrder(int orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}
