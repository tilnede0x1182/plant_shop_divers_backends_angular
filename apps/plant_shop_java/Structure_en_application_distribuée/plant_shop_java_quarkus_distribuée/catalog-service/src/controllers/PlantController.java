package controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import models.Plant;
import repositories.PlantRepository;
import security.Guards;
import util.ApiMapper;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
/**
 * Contrôleur REST pour les plantes.
 */
public class PlantController {

    @Inject
    PlantRepository repo;
    @Inject
    Guards guards;

    private static final Collator COLLATOR;
    static {
        COLLATOR = Collator.getInstance(Locale.ROOT);
        COLLATOR.setStrength(Collator.PRIMARY);
    }
    /**
     * Compare deux plantes par nom.
     * @param a Première plante
     * @param b Deuxième plante
     * @return Résultat de comparaison
     */
    private int comparePlants(Plant a, Plant b) {
        return COLLATOR.compare(a.name, b.name);
    }

    @GET
    @Path("/plants")
    /**
     * Liste toutes les plantes publiquement.
     * @return Réponse avec liste des plantes
     * @throws Exception En cas d'erreur
     */
    public Response listPublic() throws Exception {
        List<?> payload = repo.list().stream()
            .sorted(this::comparePlants)
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
        return Response.ok(payload).build();
    }

    @GET
    @Path("/admin/plants")
    /**
     * Liste toutes les plantes (admin).
     * @return Réponse avec liste des plantes
     * @throws Exception En cas d'erreur
     */
    public Response listAdmin() throws Exception {
        guards.requireAdmin();
        // Sécurise la route
        return listPublic();
        // Réutilise la logique publique
    }

    @GET
    @Path("/plants/{id}")
    /**
     * Affiche une plante par ID.
     * @param id ID de la plante
     * @return Réponse avec la plante
     * @throws Exception En cas d'erreur
     */
    public Response show(@PathParam("id") int id) throws Exception {
        Plant plant = repo.find(id);
        return plant != null
            ?
        Response.ok(ApiMapper.toPlant(plant)).build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/admin/plants")
    @Transactional
    /**
     * Crée une nouvelle plante.
     * @param plant Plante à créer
     * @return Réponse avec la plante créée
     * @throws Exception En cas d'erreur
     */
    public Response create(Plant plant) throws Exception {
        guards.requireAdmin();
        int id = repo.create(plant);
        Plant created = repo.find(id);
        return Response.status(Response.Status.CREATED)
                       .entity(ApiMapper.toPlant(created))
                       .build();
    }

    @PATCH
    @Path("/admin/plants/{id}")
    @Transactional
    /**
     * Met à jour une plante.
     * @param id ID de la plante
     * @param updatedData Données mises à jour
     * @return Réponse avec la plante mise à jour
     * @throws Exception En cas d'erreur
     */
    public Response update(@PathParam("id") int id, Plant updatedData) throws Exception {
        guards.requireAdmin();
        Plant existing = repo.find(id);
        if (existing == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (updatedData.name != null) existing.name = updatedData.name;
        if (updatedData.description != null) existing.description = updatedData.description;
        if (updatedData.price != null) existing.price = updatedData.price;
        // Le test vérifie la mise à jour du stock
        if (updatedData.stock != 0) existing.stock = updatedData.stock;
        repo.update(existing);
        return Response.ok(ApiMapper.toPlant(repo.find(id))).build();
    }

    @DELETE
    @Path("/admin/plants/{id}")
    @Transactional
    /**
     * Supprime une plante.
     * @param id ID de la plante
     * @return Réponse de succès
     * @throws Exception En cas d'erreur
     */
    public Response destroy(@PathParam("id") int id) throws Exception {
        guards.requireAdmin();
        repo.delete(id);
        return Response.ok().build();
    }
}
