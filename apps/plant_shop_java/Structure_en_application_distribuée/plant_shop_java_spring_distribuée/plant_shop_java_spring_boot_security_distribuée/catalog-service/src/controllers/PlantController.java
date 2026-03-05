package controllers;

import model.Plant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.PlantRepository;
import catalog.security.Guards;
import catalog.util.ApiMapper;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
/**
 * Contrôleur REST pour les plantes.
 */
public class PlantController {

    @Autowired
    PlantRepository repo;
    @Autowired
    Guards guards;

    // Logique de tri (identique)
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

    // --- Endpoints Publics ---

    @GetMapping("/plants")
    /**
     * Liste toutes les plantes publiquement.
     * @return Réponse avec liste des plantes
     * @throws Exception En cas d'erreur
     */
    public ResponseEntity<List<?>> listPublic() throws Exception {
        List<?> payload = repo.list().stream()
            .sorted(this::comparePlants)
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/plants/{id}")
    public ResponseEntity<Object> show(@PathVariable("id") int id) throws Exception {
        Plant plant = repo.find(id);
        return plant != null
            ? ResponseEntity.ok(ApiMapper.toPlant(plant))
            : ResponseEntity.notFound().build();
    }

    // --- Endpoints Admin (/admin/plants) ---

    @GetMapping("/admin/plants")
    public ResponseEntity<List<?>> listAdmin() throws Exception {
        guards.requireAdmin(); // Sécurise la route
        return listPublic();   // Réutilise la logique publique
    }

    @PostMapping("/admin/plants")
    public ResponseEntity<Object> create(@RequestBody Plant plant) throws Exception {
        guards.requireAdmin();
        int id = repo.create(plant);
        Plant created = repo.find(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toPlant(created));
    }

    @PatchMapping("/admin/plants/{id}")
    public ResponseEntity<Object> update(@PathVariable("id") int id, @RequestBody Plant updatedData) throws Exception {
        guards.requireAdmin();
        Plant existing = repo.find(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (updatedData.name != null) existing.name = updatedData.name;
        if (updatedData.description != null) existing.description = updatedData.description;
        if (updatedData.price != null) existing.price = updatedData.price;
        if (updatedData.stock != 0) existing.stock = updatedData.stock;

        repo.update(existing);
        return ResponseEntity.ok(ApiMapper.toPlant(repo.find(id)));
    }

    @DeleteMapping("/admin/plants/{id}")
    public ResponseEntity<Void> destroy(@PathVariable("id") int id) throws Exception {
        guards.requireAdmin();
        repo.delete(id);
        return ResponseEntity.ok().build();
    }
}
