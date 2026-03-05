package controllers;

import models.Order;
import models.OrderItem;
import models.Plant;
import models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import repositories.OrderItemRepository;
import repositories.OrderRepository;
import repositories.PlantRepository;
import security.Guards;
import utils.ApiMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    OrderRepository repo;
    @Autowired
    OrderItemRepository itemRepo;
    @Autowired
    PlantRepository plantRepo;
    @Autowired
    Guards guards;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() throws Exception {
        User currentUser = guards.requireUser();
        List<Order> orders = repo.findByUserIdOrderByCreatedAtDesc(currentUser.id);

        List<Map<String, Object>> payload = new ArrayList<>();
        for (Order order : orders) {
            List<Map<String, Object>> items = ApiMapper.toOrderItems(
                itemRepo.findByOrderId(order.id),
                plantId -> plantRepo.findById(plantId).orElse(null)
            );
            payload.add(ApiMapper.toOrder(order, items));
        }
        return ResponseEntity.ok(payload);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Object> create(@RequestBody Map<String, List<Map<String, Integer>>> body) throws Exception {
        User currentUser = guards.requireUser();
        List<Map<String, Integer>> itemsJson = body.get("items");

        if (itemsJson == null || itemsJson.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "items requis"));
        }

        try {
            Order newOrder = new Order(currentUser.id, BigDecimal.ZERO, "pending");
            Order savedOrder = repo.save(newOrder);
            BigDecimal total = BigDecimal.ZERO;

            for (Map<String, Integer> it : itemsJson) {
                total = total.add(createOrderItem(savedOrder, it));
            }
            savedOrder.total = total;
            repo.save(savedOrder);

            List<Map<String, Object>> items = ApiMapper.toOrderItems(
                itemRepo.findByOrderId(savedOrder.id),
                plantId -> plantRepo.findById(plantId).orElse(null)
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                                 .body(ApiMapper.toOrder(savedOrder, items));

        } catch (IllegalArgumentException ex) {
            // Gère les erreurs de stock ou d'ID
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> patch(@PathVariable("id") int id, @RequestBody Map<String, String> body) throws Exception {
        guards.requireAdmin();
        Order existing = repo.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        if (body.containsKey("status")) {
            existing.status = body.get("status");
            repo.save(existing);
        }

        List<Map<String, Object>> items = ApiMapper.toOrderItems(
            itemRepo.findByOrderId(id),
            plantId -> plantRepo.findById(plantId).orElse(null)
        );
        return ResponseEntity.ok(ApiMapper.toOrder(existing, items));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> destroy(@PathVariable("id") int id) throws Exception {
        guards.requireAdmin();
        // Doit supprimer les items avant la commande à cause de la clé étrangère
        itemRepo.deleteByOrderId(id);
        repo.deleteById(id);
        return ResponseEntity.ok().build(); // 200 OK attendu par le test
    }

    /**
	 * Logique privée pour créer un item et mettre à jour le stock.
	 * @param order Order Commande parente
	 * @param itemMap Map<String,Integer> Données de l'item (plantId, quantity)
	 * @return BigDecimal Sous-total de l'item
	 */
    private BigDecimal createOrderItem(Order order, Map<String, Integer> itemMap) throws Exception {
        int plantId = itemMap.get("plantId");
        int quantity = itemMap.get("quantity");
        Plant plant = plantRepo.findById(plantId).orElse(null);

        if (plant == null) throw new IllegalArgumentException("Plante " + plantId + " introuvable");
        if (plant.stock < quantity) throw new IllegalArgumentException("Stock insuffisant pour " + plant.name);

        plant.stock = plant.stock - quantity;
        plantRepo.save(plant);
        OrderItem item = new OrderItem(order.id, plantId, quantity, plant.price);
        itemRepo.save(item);

        return plant.price.multiply(BigDecimal.valueOf(quantity));
    }
}
