package order.repository;

import order.model.Order;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

abstract class OrderBaseRepository<T> {
    protected final Connection db;
    protected final String table;

    OrderBaseRepository(Connection db, String table) {
        this.db = db;
        this.table = table;
    }

    abstract T map(ResultSet rs) throws SQLException;

    public T find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

public final class OrderRepository extends OrderBaseRepository<Order> {

    public OrderRepository(Connection db) {
        super(db, "orders");
    }

    @Override
    Order map(ResultSet rs) throws SQLException {
        return new Order(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    public int create(Order order) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO orders(user_id, total, status) VALUES (?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.userId());
            ps.setBigDecimal(2, order.total());
            ps.setString(3, order.status());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void updateTotal(int id, BigDecimal total) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("UPDATE orders SET total=? WHERE id=?")) {
            ps.setBigDecimal(1, total);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateStatus(int id, String status) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("UPDATE orders SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<Order> findByUser(int userId) throws SQLException {
        List<Order> out = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM orders WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }
}
