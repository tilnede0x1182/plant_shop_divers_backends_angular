import java.math.BigDecimal;
import java.time.Instant;

public final class Order {
    int id;
    int userId;
    BigDecimal total;
    String status;
    Instant createdAt;

    public Order(int id, int userId, BigDecimal total, String status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.total = total;
        this.status = status;
        this.createdAt = createdAt;
    }
}
