package controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.text.Collator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import model.Plant;
import repository.PlantRepository;
import security.Guards;
import util.ApiMapper;

@Controller("/api")
public class PlantController {

    private final PlantRepository repo;
    private static final Collator COLLATOR;
    static {
        COLLATOR = Collator.getInstance(Locale.ROOT);
        COLLATOR.setStrength(Collator.PRIMARY);
    }

    @Inject
    public PlantController(Connection db) {
        this.repo = new PlantRepository(db);
    }

    @Get("/plants")
    public List<?> listPublic() throws Exception {
        return repo.list().stream()
            .sorted(this::comparePlants)
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
    }

    @Get("/admin/plants")
    public List<?> listAdmin(HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        return repo.list().stream()
            .sorted(this::comparePlants)
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
    }

    @Get("/plants/{id}")
    public HttpResponse<?> show(@PathVariable int id) throws Exception {
        Plant plant = repo.find(id);
        return plant != null ? HttpResponse.ok(ApiMapper.toPlant(plant)) : HttpResponse.notFound();
    }

    @Post("/admin/plants")
    public HttpResponse<?> create(@Body Plant plant, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        int id = repo.create(plant);
        Plant created = repo.find(id);
        return HttpResponse.created(ApiMapper.toPlant(created));
    }

    @Patch("/admin/plants/{id}")
    public HttpResponse<?> update(@PathVariable int id, @Body Plant updatedData, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        Plant existing = repo.find(id);
        if (existing == null) return HttpResponse.notFound();

        if (updatedData.name != null) existing.name = updatedData.name;
        if (updatedData.description != null) existing.description = updatedData.description;
        if (updatedData.price != null) existing.price = updatedData.price;
        if (updatedData.stock != 0) existing.stock = updatedData.stock;

        repo.update(existing);
        return HttpResponse.ok(ApiMapper.toPlant(repo.find(id)));
    }

    @Delete("/admin/plants/{id}")
    public HttpResponse<?> destroy(@PathVariable int id, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        repo.delete(id);
        return HttpResponse.ok();
    }

    @Patch("/internal/plants/{id}/stock")
    public HttpResponse<?> updateStock(@PathVariable int id, @Body java.util.Map<String, Integer> body) throws Exception {
        Plant plant = repo.find(id);
        if (plant == null) return HttpResponse.notFound();
        if (!body.containsKey("stock")) {
            return HttpResponse.badRequest(java.util.Map.of("error", "Champ stock requis"));
        }
        int newStock = body.get("stock");
        repo.updateStock(id, newStock);
        return HttpResponse.ok(java.util.Map.of("success", true, "stock", newStock));
    }

    private int comparePlants(Plant a, Plant b) {
        return COLLATOR.compare(a.name, b.name);
    }
}
