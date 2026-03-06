package order.model;

import java.math.BigDecimal;

/**
 * Représente une plante avec son stock.
 * @param id ID de la plante
 * @param name Nom de la plante
 * @param price Prix
 * @param stock Stock disponible
 */
public record PlantStock(
    int id,
    String name,
    BigDecimal price,
    int stock
) {}
