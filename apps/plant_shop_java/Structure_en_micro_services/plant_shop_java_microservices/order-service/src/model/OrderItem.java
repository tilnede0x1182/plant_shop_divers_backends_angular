package order.model;

import java.math.BigDecimal;

public record OrderItem(
    int id,
    int orderId,
    int plantId,
    int quantity,
    BigDecimal price
) {}
