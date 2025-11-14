package repository;

import io.micronaut.context.annotation.Bean;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Order;

@Singleton
public final class OrderRepository extends BaseRepository<Order> {

    public OrderRepository(Connection db) {
        super(db, "orders");
    }

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

    public void updateTotal(int id, BigDecimal total) throws SQLException {
        String sql = "UPDATE orders SET total=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE orders SET status=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

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
