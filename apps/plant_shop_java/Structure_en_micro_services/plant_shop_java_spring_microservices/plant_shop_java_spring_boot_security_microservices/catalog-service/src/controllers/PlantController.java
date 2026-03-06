package controllers;

import model.Plant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repository.PlantRepository;
import security.Guards;
import util.ApiMapper;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion des plantes.
 * Expose les endpoints publics et admin pour le catalogue.
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
     * Compare deux plantes par leur nom pour le tri.
     * @param a Première plante
     * @param b Deuxième plante
     * @return Résultat de la comparaison
     */
    private int comparePlants(Plant a, Plant b) {
        return COLLATOR.compare(a.name, b.name);
    }

    // --- Endpoints Publics ---

    /**
     * Liste toutes les plantes triées par nom.
     * @return Liste des plantes au format JSON
     * @throws Exception En cas d'erreur SQL
     */
    @GetMapping("/plants")
    public ResponseEntity<List<?>> listPublic() throws Exception {
        List<?> payload = repo.findAll().stream()
            .sorted(this::comparePlants)
            .map(ApiMapper::toPlant)
            .collect(Collectors.toList());
        return ResponseEntity.ok(payload);
    }

    /**
     * Affiche une plante par son identifiant.
     * @param id Identifiant de la plante
     * @return La plante ou 404 si non trouvée
     * @throws Exception En cas d'erreur SQL
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
     * @return Liste des plantes au format JSON
     * @throws Exception En cas d'erreur SQL
     */
    @GetMapping("/admin/plants")
    public ResponseEntity<List<?>> listAdmin() throws Exception {
        guards.requireAdmin(); // Sécurise la route
        return listPublic();   // Réutilise la logique publique
    }

    /**
     * Crée une nouvelle plante (admin requis).
     * @param plant Données de la plante à créer
     * @return La plante créée avec son identifiant
     * @throws Exception En cas d'erreur SQL
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
     * Met à jour une plante existante (admin requis).
     * @param id Identifiant de la plante
     * @param updatedData Données à mettre à jour
     * @return La plante mise à jour ou 404
     * @throws Exception En cas d'erreur SQL
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
     * Supprime une plante (admin requis).
     * @param id Identifiant de la plante à supprimer
     * @return 200 OK si supprimée
     * @throws Exception En cas d'erreur SQL
     */
    @DeleteMapping("/admin/plants/{id}")
    public ResponseEntity<Void> destroy(@PathVariable("id") int id) throws Exception {
        guards.requireAdmin();
        repo.delete(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Met à jour le stock d'une plante (endpoint interne).
     * @param id Identifiant de la plante
     * @param body Map contenant le nouveau stock
     * @return Confirmation ou erreur
     * @throws Exception En cas d'erreur SQL
     */
    @PatchMapping("/internal/plants/{id}/stock")
    public ResponseEntity<Object> updateStock(@PathVariable("id") int id, @RequestBody java.util.Map<String, Integer> body) throws Exception {
        Plant plant = repo.find(id);
        if (plant == null) {
            return ResponseEntity.notFound().build();
        }
        if (!body.containsKey("stock")) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "Champ stock requis"));
        }
        int newStock = body.get("stock");
        repo.updateStock(id, newStock);
        return ResponseEntity.ok(java.util.Map.of("success", true, "stock", newStock));
    }
}
