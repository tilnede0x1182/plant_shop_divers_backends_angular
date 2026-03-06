package model;

import java.math.BigDecimal;

/**
 * DTO minimal pour les informations des plantes nécessaires au order-service.
 * Dans une architecture microservices, order-service ne charge que les données minimales
 * depuis la BD partagée pour vérifier prix/stock.
 */
public final class PlantStock {
    public final int        id;
    public final String     name;
    public final BigDecimal price;
    public final int        stock;

    /**
     * Constructeur du DTO PlantStock.
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
