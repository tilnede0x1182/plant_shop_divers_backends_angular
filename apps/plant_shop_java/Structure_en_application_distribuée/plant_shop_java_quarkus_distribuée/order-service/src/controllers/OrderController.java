package controllers;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import models.Order;
import models.OrderItem;
import models.Plant;
import models.User;
import repositories.OrderItemRepository;
import repositories.OrderRepository;
import repositories.PlantRepository;
import security.Guards;
import util.ApiMapper;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
/**
 * Contrôleur REST pour les commandes.
 */
@RequestScoped
public class OrderController {

    @Inject
    OrderRepository repo;
    @Inject
    OrderItemRepository itemRepo;
    @Inject
    PlantRepository plantRepo;
    @Inject
    Guards guards;

    /**
     * Liste les commandes de l'utilisateur.
     * @return Réponse avec liste des commandes
     * @throws Exception En cas d'erreur
     */
    @GET
    public Response list() throws Exception {
        User currentUser = guards.requireUser();
        List<Order> orders = repo.listByUser(currentUser.id);

        orders.sort(Comparator.comparing(o -> o.createdAt, Comparator.reverseOrder()));

        List<Map<String, Object>> payload = new ArrayList<>();
        for (Order order : orders) {
            List<Map<String, Object>> items = ApiMapper.toOrderItems(
                itemRepo.listByOrder(order.id),
                plantRepo::find // Utilise la lambda pour le PlantLookup
            );
            payload.add(ApiMapper.toOrder(order, items));
        }
        return Response.ok(payload).build();
    }

    /**
     * Crée une nouvelle commande.
     * @param body Données de la commande
     * @return Réponse avec la commande créée
     * @throws Exception En cas d'erreur
     */
    @POST
    @Transactional // Gère la transaction (commit/rollback)
    public Response create(Map<String, List<Map<String, Integer>>> body) throws Exception {
        User currentUser = guards.requireUser();
        List<Map<String, Integer>> itemsJson = body.get("items");

        if (itemsJson == null || itemsJson.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", "items requis"))
                           .build();
        }

        try {
            Order newOrder = new Order(currentUser.id, BigDecimal.ZERO, "pending");
            int orderId = repo.create(newOrder);
            BigDecimal total = BigDecimal.ZERO;

            for (Map<String, Integer> it : itemsJson) {
                total = total.add(createOrderItem(orderId, it));
            }
            repo.updateTotal(orderId, total);

            Order finalOrder = repo.find(orderId);
            List<Map<String, Object>> items = ApiMapper.toOrderItems(
                itemRepo.listByOrder(orderId),
                plantRepo::find
            );
            return Response.status(Response.Status.CREATED)
                           .entity(ApiMapper.toOrder(finalOrder, items))
                           .build();
        } catch (IllegalArgumentException ex) {
            // @Transactional gère le rollback, mais on envoie la 400
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(Map.of("error", ex.getMessage()))
                           .build();
        }
        // Les autres exceptions lèveront une 500 et @Transactional gèrera le rollback
    }

    /**
     * Met à jour une commande.
     * @param id ID de la commande
     * @param body Données à mettre à jour
     * @return Réponse avec la commande mise à jour
     * @throws Exception En cas d'erreur
     */
    @PATCH
    @Path("/{id}")
    @Transactional
    public Response patch(@PathParam("id") int id, Map<String, String> body) throws Exception {
        guards.requireAdmin();
        if (repo.find(id) == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (body.containsKey("status")) {
            repo.updateStatus(id, body.get("status"));
        }

        Order updated = repo.find(id);
        List<Map<String, Object>> items = ApiMapper.toOrderItems(
            itemRepo.listByOrder(id),
            plantRepo::find
        );
        return Response.ok(ApiMapper.toOrder(updated, items)).build();
    }

    /**
     * Supprime une commande.
     * @param id ID de la commande
     * @return Réponse de succès
     * @throws Exception En cas d'erreur
     */
    @DELETE
    @Path("/{id}")
    @Transactional
    public Response destroy(@PathParam("id") int id) throws Exception {
        guards.requireAdmin();
        // Doit supprimer les items avant la commande à cause de la clé étrangère
        itemRepo.deleteByOrder(id);
        repo.delete(id);
        return Response.ok().build(); // 200 OK attendu par le test
    }

    /**
     * Logique privée pour créer un item et mettre à jour le stock.
     * Réutilise la logique de Micronaut.
     */
    private BigDecimal createOrderItem(int orderId, Map<String, Integer> itemMap) throws Exception {
        int plantId = itemMap.get("plantId");
        int quantity = itemMap.get("quantity");
        Plant plant = plantRepo.find(plantId);

        if (plant == null) throw new IllegalArgumentException("Plante " + plantId + " introuvable");
        if (plant.stock < quantity) throw new IllegalArgumentException("Stock insuffisant pour " + plant.name);

        plantRepo.updateStock(plant.id, plant.stock - quantity);
        OrderItem item = new OrderItem(orderId, plantId, quantity, plant.price);
        itemRepo.create(item);

        return plant.price.multiply(BigDecimal.valueOf(quantity));
    }
}
