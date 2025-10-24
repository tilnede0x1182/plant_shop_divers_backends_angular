package repository;

import java.sql.*;
import java.util.*;
import model.OrderItem;

public final class OrderItemRepository {

    private final Connection db;

    public OrderItemRepository(Connection db) { this.db = db; }

    public void addItem(OrderItem it) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "INSERT INTO order_items(order_id,plant_id,quantity,price) VALUES (?,?,?,?)");
        ps.setInt(1, it.orderId);
        ps.setInt(2, it.plantId);
        ps.setInt(3, it.quantity);
        ps.setBigDecimal(4, it.price);
        ps.executeUpdate();
    }

    public List<OrderItem> listByOrder(int orderId) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "SELECT * FROM order_items WHERE order_id=?");
        ps.setInt(1, orderId);
        ResultSet rs = ps.executeQuery();
        List<OrderItem> out = new ArrayList<OrderItem>();
        while (rs.next())
            out.add(new OrderItem(rs.getInt("id"),
                                  rs.getInt("order_id"),
                                  rs.getInt("plant_id"),
                                  rs.getInt("quantity"),
                                  rs.getBigDecimal("price")));
        return out;
    }

    public void deleteByOrder(int orderId) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "DELETE FROM order_items WHERE order_id=?");
        ps.setInt(1, orderId);
        ps.executeUpdate();
    }
}
