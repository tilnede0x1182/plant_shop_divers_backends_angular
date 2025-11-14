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
import model.Plant;
import repository.PlantRepository;
import utils.ApiMapper;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class PlantController {

    @Inject
    PlantRepository repo;

    private static final Collator COLLATOR;
    static {
        COLLATOR = Collator.getInstance(Locale.ROOT);
        COLLATOR.setStrength(Collator.PRIMARY);
    }
    private int comparePlants(Plant a, Plant b) {
        return COLLATOR.compare(a.name, b.name);
    }

    @GET
    @Path("/plants")
    public Response listPublic() throws Exception {
        List<?> payload = repo.list().stream()
            .sorted(this::comparePlants)
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
        return Response.ok(payload).build();
    }

    @GET
    @Path("/admin/plants")
    public Response listAdmin() throws Exception {
        return listPublic(); // Réutilise la logique publique
    }

    @GET
    @Path("/plants/{id}")
    public Response show(@PathParam("id") int id) throws Exception {
        Plant plant = repo.find(id);
        return plant != null
            ? Response.ok(ApiMapper.toPlant(plant)).build()
            : Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/admin/plants")
    @Transactional
    public Response create(Plant plant) throws Exception {
        int id = repo.create(plant);
        Plant created = repo.find(id);
        return Response.status(Response.Status.CREATED)
                       .entity(ApiMapper.toPlant(created))
                       .build();
    }

    @PATCH
    @Path("/admin/plants/{id}")
    @Transactional
    public Response update(@PathParam("id") int id, Plant updatedData) throws Exception {
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
    public Response destroy(@PathParam("id") int id) throws Exception {
        repo.delete(id);
        return Response.ok().build();
    }
}
