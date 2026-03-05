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

/**
 * Contrôleur pour les plantes Micronaut.
 */
@Controller("/api")
public class PlantController {

    private final PlantRepository repo;
    private static final Collator COLLATOR;
    static {
        COLLATOR = Collator.getInstance(Locale.ROOT);
        COLLATOR.setStrength(Collator.PRIMARY);
    }

    /**
     * Constructeur avec injection.
     * @param db Connection Connexion DB
     */
    @Inject
    public PlantController(Connection db) {
        this.repo = new PlantRepository(db);
    }

    /**
     * Liste les plantes (public).
     * @return List Liste de plantes
     * @throws Exception En cas d erreur
     */
    @Get("/plants")
    public List<?> listPublic() throws Exception {
        return repo.list().stream()
            .sorted(this::comparePlants)
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
    }

    /**
     * Liste les plantes (admin).
     * @param request HttpRequest Requête HTTP
     * @return List Liste de plantes
     * @throws Exception En cas d erreur
     */
    @Get("/admin/plants")
    public List<?> listAdmin(HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        return repo.list().stream()
            .sorted(this::comparePlants)
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
    }

    /**
     * Affiche une plante.
     * @param id int ID plante
     * @return HttpResponse Réponse HTTP
     * @throws Exception En cas d erreur
     */
    @Get("/plants/{id}")
    public HttpResponse<?> show(@PathVariable int id) throws Exception {
        Plant plant = repo.find(id);
        return plant != null ? HttpResponse.ok(ApiMapper.toPlant(plant)) : HttpResponse.notFound();
    }

    /**
     * Crée une plante.
     * @param plant Plant Données plante
     * @param request HttpRequest Requête HTTP
     * @return HttpResponse Réponse HTTP
     * @throws Exception En cas d erreur
     */
    @Post("/admin/plants")
    public HttpResponse<?> create(@Body Plant plant, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        int id = repo.create(plant);
        Plant created = repo.find(id);
        return HttpResponse.created(ApiMapper.toPlant(created));
    }

    /**
     * Met à jour une plante.
     * @param id int ID plante
     * @param updatedData Plant Nouvelles données
     * @param request HttpRequest Requête HTTP
     * @return HttpResponse Réponse HTTP
     * @throws Exception En cas d erreur
     */
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

    /**
     * Supprime une plante.
     * @param id int ID plante
     * @param request HttpRequest Requête HTTP
     * @return HttpResponse Réponse HTTP
     * @throws Exception En cas d erreur
     */
    @Delete("/admin/plants/{id}")
    public HttpResponse<?> destroy(@PathVariable int id, HttpRequest<?> request) throws Exception {
        Guards.requireAdmin(request);
        repo.delete(id);
        return HttpResponse.ok();
    }

    /**
     * Compare deux plantes pour le tri.
     * @param a Plant Première plante
     * @param b Plant Deuxième plante
     * @return int Résultat comparaison
     */
    private int comparePlants(Plant a, Plant b) {
        return COLLATOR.compare(a.name, b.name);
    }
}
