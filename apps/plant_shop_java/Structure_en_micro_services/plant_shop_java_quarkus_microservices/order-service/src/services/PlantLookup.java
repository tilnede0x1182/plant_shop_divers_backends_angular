package services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import models.Plant;
import util.ApiMapper;

/**
 * Implémentation d'ApiMapper.PlantLookup reposant sur CatalogClient.
 * Garantit qu'OrderController ne dépend que d'un contrat HTTP.
 */
@ApplicationScoped
public class PlantLookup implements ApiMapper.PlantLookup {

    @Inject
    CatalogClient catalogClient;

    @Override
    public Plant find(int id) throws Exception {
        return catalogClient.fetchPlant(id);
    }

    /**
     * Réserve le stock de la plante demandée via catalog-service.
     */
    public Plant reserveStock(int plantId, int quantity) throws Exception {
        Plant plant = find(plantId);
        if (plant == null) {
            throw new IllegalArgumentException("Plante " + plantId + " introuvable");
        }
        if (plant.stock < quantity) {
            throw new IllegalArgumentException("Stock insuffisant pour " + plant.name);
        }
        int remaining = plant.stock - quantity;
        catalogClient.updateStock(plantId, remaining);
        plant.stock = remaining;
        return plant;
    }
}
