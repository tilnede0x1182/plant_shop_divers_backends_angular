package controllers;

import models.Plant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repositories.PlantRepository;
import security.Guards;
import utils.ApiMapper;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion des plantes.
 * Fournit les endpoints publics et admin pour le catalogue.
 */
@RestController
@RequestMapping("/api")
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
     * Compare deux plantes par nom (insensible à la casse).
     *
     * @param a Plant Première plante
     * @param b Plant Deuxième plante
     * @return int Résultat de la comparaison
     */
    private int comparePlants(Plant a, Plant b) {
        return COLLATOR.compare(a.name, b.name);
    }

    // --- Endpoints Publics ---

    /**
     * Liste toutes les plantes (endpoint public).
     * @return 200 avec la liste des plantes triées par nom
     */
    @GetMapping("/plants")
    public ResponseEntity<List<?>> listPublic() throws Exception {
        List<?> payload = repo.list().stream()
            .sorted(this::comparePlants)
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    /**
     * Récupère une plante par son ID.
     * @param id ID de la plante
     * @return 200 avec la plante, 404 si non trouvée
     */
    @GetMapping("/plants/{id}")
    public ResponseEntity<Object> show(@PathVariable("id") int id) throws Exception {
        Plant plant = repo.find(id);
        return plant != null
            ? ResponseEntity.ok(ApiMapper.toPlant(plant))
            : ResponseEntity.notFound().build();
    }

    // --- Endpoints Admin (/admin/plants) ---

    /**
     * Liste toutes les plantes (endpoint admin).
     * @return 200 avec la liste des plantes
     */
    @GetMapping("/admin/plants")
    public ResponseEntity<List<?>> listAdmin() throws Exception {
        guards.requireAdmin(); // Sécurise la route
        return listPublic();   // Réutilise la logique publique
    }

    /**
     * Crée une nouvelle plante (admin uniquement).
     * @param plant Données de la plante
     * @return 201 avec la plante créée
     */
    @PostMapping("/admin/plants")
    public ResponseEntity<Object> create(@RequestBody Plant plant) throws Exception {
        guards.requireAdmin();
        int id = repo.create(plant);
        Plant created = repo.find(id);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toPlant(created));
    }

    /**
     * Met à jour une plante (admin uniquement).
     * @param id ID de la plante
     * @param updatedData Nouvelles données
     * @return 200 avec la plante modifiée, 404 si non trouvée
     */
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

    /**
     * Supprime une plante (admin uniquement).
     * @param id ID de la plante
     * @return 200 OK
     */
    @DeleteMapping("/admin/plants/{id}")
    public ResponseEntity<Void> destroy(@PathVariable("id") int id) throws Exception {
        guards.requireAdmin();
        repo.delete(id);
        return ResponseEntity.ok().build();
    }
}
