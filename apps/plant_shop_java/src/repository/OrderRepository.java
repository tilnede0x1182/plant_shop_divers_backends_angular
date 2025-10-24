package repository;

import java.sql.*;
import java.util.*;
import model.Order;

public final class OrderRepository {

    private final Connection db;

    public OrderRepository(Connection db) { this.db = db; }

    public int create(Order o) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "INSERT INTO orders(user_id,total,status) VALUES (?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, o.userId);
        ps.setBigDecimal(2, o.total);
        ps.setString(3, o.status);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys(); rs.next();
        return rs.getInt(1);
    }

    public Order find(int id) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "SELECT * FROM orders WHERE id=?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return null;
        return new Order(rs.getInt("id"),
                         rs.getInt("user_id"),
                         rs.getBigDecimal("total"),
                         rs.getString("status"),
                         rs.getTimestamp("created_at"));
    }

    public List<Order> list() throws SQLException {
        Statement st = db.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM orders");
        List<Order> out = new ArrayList<Order>();
        while (rs.next())
            out.add(new Order(rs.getInt("id"),
                              rs.getInt("user_id"),
                              rs.getBigDecimal("total"),
                              rs.getString("status"),
                              rs.getTimestamp("created_at")));
        return out;
    }

    public void updateTotal(int id, java.math.BigDecimal total) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "UPDATE orders SET total=? WHERE id=?");
        ps.setBigDecimal(1, total);
        ps.setInt(2, id);
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        PreparedStatement ps = db.prepareStatement("DELETE FROM orders WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
