package model;
import java.math.BigDecimal;
import java.sql.Timestamp;

public final class Order {
    public int id;
    public int userId;
    public BigDecimal total;
    public String status;
    public Timestamp createdAt;

    public Order(int id, int userId, BigDecimal total, String status, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }
    public Order(int userId, BigDecimal total, String status) {
        this(0, userId, total, status, null);
    }
    public Order() {} // Nécessaire pour la désérialisation JSON
}
