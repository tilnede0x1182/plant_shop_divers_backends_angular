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

    /**
     * Recherche une plante par son ID via le service de catalogue.
     *
     * @param id ID de la plante
     * @return Objet Plant ou null si non trouvee
     * @throws Exception En cas d'erreur HTTP
     */
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
