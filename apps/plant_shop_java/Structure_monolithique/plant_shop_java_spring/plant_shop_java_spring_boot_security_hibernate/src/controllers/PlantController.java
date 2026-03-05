package controllers;

import models.Plant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import security.Guards;
import utils.ApiMapper;
import repositories.PlantRepository;

import java.util.List;
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

    // --- Endpoints Publics ---

    /** Liste toutes les plantes (endpoint public). */
    @GetMapping("/plants")
    public ResponseEntity<List<?>> listPublic() throws Exception {
        List<?> payload = repo.findAllByOrderByNameAsc().stream()
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    /** Récupère une plante par son ID. */
    @GetMapping("/plants/{id}")
    public ResponseEntity<Object> show(@PathVariable("id") int id) throws Exception {
        Plant plant = repo.findById(id).orElse(null);
        if (plant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiMapper.toPlant(plant));
    }

    // --- Endpoints Admin (/admin/plants) ---

    /** Liste toutes les plantes (endpoint admin). */
    @GetMapping("/admin/plants")
    public ResponseEntity<List<?>> listAdmin() throws Exception {
        guards.requireAdmin(); // Sécurise la route
        return listPublic();   // Réutilise la logique publique
    }

    /** Crée une nouvelle plante (admin uniquement). */
    @PostMapping("/admin/plants")
    public ResponseEntity<Object> create(@RequestBody Plant plant) throws Exception {
        guards.requireAdmin();
        if (plant.description == null) {
            plant.description = "";
        }
        Plant created = repo.save(plant);
        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(ApiMapper.toPlant(created));
    }

    /** Met à jour une plante (admin uniquement). */
    @PatchMapping("/admin/plants/{id}")
    public ResponseEntity<Object> update(@PathVariable("id") int id, @RequestBody Plant updatedData) throws Exception {
        guards.requireAdmin();
        Plant existing = repo.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (updatedData.name != null) existing.name = updatedData.name;
        if (updatedData.description != null) existing.description = updatedData.description;
        if (updatedData.price != null) existing.price = updatedData.price;
        if (updatedData.stock != 0) existing.stock = updatedData.stock;
        repo.save(existing);
        return ResponseEntity.ok(ApiMapper.toPlant(existing));
    }

    /** Supprime une plante (admin uniquement). */
    @DeleteMapping("/admin/plants/{id}")
    public ResponseEntity<Void> destroy(@PathVariable("id") int id) throws Exception {
        guards.requireAdmin();
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
