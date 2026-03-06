package repository;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Order;

/**
 * Repository pour la gestion des commandes en base.
 */
public final class OrderRepository {

    private final Connection db;

    /**
     * Construit le repository avec la connexion BDD.
     * @param db Connexion à la base de données
     */
    public OrderRepository(Connection db) {
        this.db = db;
    }

    /**
     * Recherche une commande par son ID.
     * @param id Identifiant de la commande
     * @return Commande trouvée ou null
     * @throws SQLException En cas d'erreur BDD
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
     * Supprime une commande par son ID.
     * @param id Identifiant de la commande
     * @throws SQLException En cas d'erreur BDD
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM orders WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Convertit un ResultSet en objet Order.
     * @param rs ResultSet positionné sur une ligne
     * @return Objet Order correspondant
     * @throws SQLException En cas d'erreur de lecture
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
     * Insère une nouvelle commande en base.
     * @param o Commande à insérer
     * @return ID généré de la commande
     * @throws SQLException En cas d'erreur d'insertion
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
     * Met à jour le montant total d'une commande.
     * @param id Identifiant de la commande
     * @param total Nouveau montant total
     * @throws SQLException En cas d'erreur BDD
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
     * Met à jour uniquement le statut d'une commande.
     * @param id L'identifiant de la commande.
     * @param status Le nouveau statut.
     * @throws SQLException
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
     * @throws SQLException En cas d'erreur BDD
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
}
