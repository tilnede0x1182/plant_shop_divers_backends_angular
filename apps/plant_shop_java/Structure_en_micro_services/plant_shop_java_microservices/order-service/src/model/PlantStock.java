package order.model;

import java.math.BigDecimal;

public record PlantStock(
    int id,
    String name,
    BigDecimal price,
    int stock
) {}
