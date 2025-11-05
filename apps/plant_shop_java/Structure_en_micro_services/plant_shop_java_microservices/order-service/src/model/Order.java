package order.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Order(
    int id,
    int userId,
    BigDecimal total,
    String status,
    Instant createdAt
) {
    public Order(int id, int userId, BigDecimal total) {
        this(id, userId, total, "pending", Instant.now());
    }

    public Order withTotal(BigDecimal value) {
        return new Order(id, userId, value, status, createdAt);
    }

    public Order withStatus(String value) {
        return new Order(id, userId, total, value, createdAt);
    }
}
