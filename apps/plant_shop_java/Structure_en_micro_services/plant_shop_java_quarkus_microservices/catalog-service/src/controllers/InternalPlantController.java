package controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import models.Plant;
import repositories.PlantRepository;
import util.ApiMapper;

/**
 * Routes internes accessibles par les autres services (catalog client).
 * Permet de mettre à jour le stock sans exposer l’intégralité du repository.
 */
@Path("/internal/plants")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class InternalPlantController {

    @Inject
    PlantRepository repo;

    /**
     * Met à jour le stock d'une plante.
     * Endpoint interne appelé par le service de commandes.
     *
     * @param id ID de la plante
     * @param payload Map contenant la clé "stock" avec la nouvelle valeur
     * @return Réponse HTTP avec la plante mise à jour
     * @throws Exception En cas d'erreur lors de la mise à jour
     */
    @PATCH
    @Path("/{id}/stock")
    @Transactional
    public Response updateStock(@PathParam("id") int id, Map<String, Integer> payload) throws Exception {
        Plant existing = repo.find(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(Map.of("error", "Plante " + id + " introuvable"))
                           .build();
        }

        if (payload == null || !payload.containsKey("stock")) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "stock requis"))
                           .build();
        }
        Integer stock = payload.get("stock");
        if (stock == null || stock < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "stock invalide"))
                           .build();
        }

        existing.stock = stock;
        repo.update(existing);
        return Response.ok(ApiMapper.toPlant(repo.find(id))).build();
    }
}
