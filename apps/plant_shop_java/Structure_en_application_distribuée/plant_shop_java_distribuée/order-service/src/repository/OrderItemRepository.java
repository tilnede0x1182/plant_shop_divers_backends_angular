import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class OrderItemRepository extends OrderBaseRepository<OrderItem> {

    public OrderItemRepository(Connection db) {
        super(db, "order_items");
    }

    @Override
    OrderItem map(ResultSet rs) throws SQLException {
        return new OrderItem(
            rs.getInt("id"),
            rs.getInt("order_id"),
            rs.getInt("plant_id"),
            rs.getInt("quantity"),
            rs.getBigDecimal("price")
        );
    }

    public int create(OrderItem item) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO order_items(order_id, plant_id, quantity, price) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, item.orderId);
            ps.setInt(2, item.plantId);
            ps.setInt(3, item.quantity);
            ps.setBigDecimal(4, item.price);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<OrderItem> listByOrder(int orderId) throws SQLException {
        List<OrderItem> out = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM order_items WHERE order_id=?")) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
        }
        return out;
    }

    public void deleteByOrder(int orderId) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM order_items WHERE order_id=?")) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
}

final class PlantRepository {
    private final Connection db;

    PlantRepository(Connection db) {
        this.db = db;
    }

    Plant find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM plants WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Plant(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock")
                    );
                }
            }
        }
        return null;
    }

    void updateStock(int id, int stock) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("UPDATE plants SET stock=? WHERE id=?")) {
            ps.setInt(1, stock);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
