package services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import models.Plant;
import repositories.PlantRepository;
import util.ApiMapper;
/**
 * Service permettant de récupérer les informations d'une plante.
 * Implémente l'interface fonctionnelle définie dans ApiMapper pour la génération du JSON.
 */
@ApplicationScoped
public class PlantLookup implements ApiMapper.PlantLookup {

    @Inject
    PlantRepository repository;

    @Override
    public Plant find(int id) throws Exception {
        return repository.find(id);
    }
}
