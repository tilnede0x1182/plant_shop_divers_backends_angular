package model;
import java.math.BigDecimal;

/**
 * DTO représentant une plante avec son stock.
 * Utilisé pour les vérifications de disponibilité lors des commandes.
 */
public final class PlantStock {
    public final int id;
    public final String name;
    public final BigDecimal price;
    public final int stock;

    /**
     * Constructeur pour une plante avec son stock.
     * @param id Identifiant de la plante
     * @param name Nom de la plante
     * @param price Prix unitaire
     * @param stock Quantité en stock
     */
    public PlantStock(int id, String name, BigDecimal price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}
