package repository;

import java.sql.*;
import java.math.BigDecimal;
import model.Order;

/**
 * Repository pour les commandes.
 */
public final class OrderRepository extends BaseRepository<Order> {

    /**
     * Constructeur.
     * @param db Connection Connexion DB
     */
    public OrderRepository(Connection db) {
        super(db, "orders");
    }

    /**
     * Mappe un ResultSet vers un Order.
     * @param rs ResultSet Résultat SQL
     * @return Order Commande créée
     * @throws SQLException En cas d erreur SQL
     */
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

    // Les méthodes `find(id)`, `list()` et `delete(id)` sont maintenant héritées de BaseRepository.

    /**
     * Crée une commande.
     * @param o Order Commande à créer
     * @return int ID généré
     * @throws SQLException En cas d erreur SQL
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
     * Met à jour le total d une commande.
     * @param id int ID de la commande
     * @param total BigDecimal Nouveau total
     * @throws SQLException En cas d erreur SQL
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
}
