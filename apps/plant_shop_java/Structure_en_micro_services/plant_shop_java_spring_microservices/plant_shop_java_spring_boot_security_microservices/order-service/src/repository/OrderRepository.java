package repository;

import model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour la gestion des commandes en base de données.
 * Fournit les opérations CRUD sur la table orders.
 */
@Repository
@RequestScope
public class OrderRepository  {

    @Autowired
    private final Connection db;

    /**
     * Constructeur avec injection de la connexion BDD.
     * @param db Connexion à la base de données
     */
    public OrderRepository(Connection db) {
        this.db = db;
    }

    /**
     * Mappe un ResultSet vers un objet Order.
     * @param rs ResultSet positionné sur une ligne
     * @return La commande mappée
     * @throws SQLException En cas d'erreur SQL
     */
    private Order mapFromResultSet(ResultSet rs) throws SQLException {
        return new Order(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getTimestamp("created_at")
        );
    }

    /**
     * Crée une nouvelle commande en base de données.
     * @param o Commande à créer
     * @return L'identifiant généré
     * @throws SQLException En cas d'erreur SQL
     */
    public int create(Order o) throws SQLException {
        String sql = "INSERT INTO orders(user_id, total, status) VALUES (?, ?, ?)";
        try (PreparedStatement ps = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.userId);
            ps.setBigDecimal(2, o.total);
            ps.setString(3, o.status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Met à jour le total d'une commande.
     * @param id Identifiant de la commande
     * @param total Nouveau montant total
     * @throws SQLException En cas d'erreur SQL
     */
    public void updateTotal(int id, BigDecimal total) throws SQLException {
        String sql = "UPDATE orders SET total=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Met à jour le statut d'une commande.
     * @param id Identifiant de la commande
     * @param status Nouveau statut
     * @throws SQLException En cas d'erreur SQL
     */
    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE orders SET status=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Liste les commandes d'un utilisateur.
     * @param userId Identifiant de l'utilisateur
     * @return Liste des commandes
     * @throws SQLException En cas d'erreur SQL
     */
    public List<Order> listByUser(int userId) throws SQLException {
        List<Order> results = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE user_id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapFromResultSet(rs));
                }
            }
        }
        return results;
    }

    /**
     * Recherche une commande par son identifiant.
     * @param id Identifiant de la commande
     * @return La commande trouvée ou null
     * @throws SQLException En cas d'erreur SQL
     */
    public Order find(int id) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id=?";
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
     * Supprime une commande par son identifiant.
     * @param id Identifiant de la commande
     * @throws SQLException En cas d'erreur SQL
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM orders WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Récupère toutes les commandes.
     * @return Liste des commandes triées par id
     * @throws SQLException En cas d'erreur SQL
     */
    public List<Order> findAll() throws SQLException {
        String sql = "SELECT * FROM orders ORDER BY id";
        List<Order> orders = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                orders.add(mapFromResultSet(rs));
            }
        }
        return orders;
    }
}
