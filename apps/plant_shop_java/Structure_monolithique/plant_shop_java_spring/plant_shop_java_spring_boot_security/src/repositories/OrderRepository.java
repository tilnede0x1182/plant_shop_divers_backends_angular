package repositories;

import models.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour les commandes.
 */
@Repository
@RequestScope
public class OrderRepository extends BaseRepository<Order> {

    /** Constructeur avec injection. */
    @Autowired
    public OrderRepository(Connection db) {
        super(db, "orders");
    }

    /** {@inheritDoc} */
    @Override
    protected Order mapFromResultSet(ResultSet rs) throws SQLException {
        return new Order(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getTimestamp("created_at")
        );
    }

    /**
     * Crée une nouvelle commande.
     *
     * @param o Order La commande à créer
     * @return int L'ID généré
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
     *
     * @param id int Identifiant de la commande
     * @param total BigDecimal Nouveau total
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
     *
     * @param id int Identifiant de la commande
     * @param status String Nouveau statut
     */
    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE orders SET status=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Liste les commandes d'un utilisateur. */
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
