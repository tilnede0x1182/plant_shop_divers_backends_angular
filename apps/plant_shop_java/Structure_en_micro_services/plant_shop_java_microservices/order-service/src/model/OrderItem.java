package order.model;

import java.math.BigDecimal;

/**
 * Représente un item de commande.
 * @param id ID de l'item
 * @param orderId ID de la commande
 * @param plantId ID de la plante
 * @param quantity Quantité
 * @param price Prix unitaire
 */
public record OrderItem(
    int id,
    int orderId,
    int plantId,
    int quantity,
    BigDecimal price
) {}
