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
import security.Guards;
import services.PlantLookup;
import util.ApiMapper;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
/**
 * Controleur REST pour la gestion des commandes.
 * Expose les endpoints CRUD pour les commandes utilisateur.
 */
@RequestScoped
public class OrderController {

    @Inject
    OrderRepository repo;
    @Inject
    OrderItemRepository itemRepo;
    @Inject
    PlantLookup plantLookup;
    @Inject
    Guards guards;

    /**
     * Liste les commandes de l'utilisateur connecte.
     *
     * @return Reponse HTTP avec la liste des commandes
     * @throws Exception En cas d'erreur lors de la recuperation
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
                plantLookup
            );
            payload.add(ApiMapper.toOrder(order, items));
        }
        return Response.ok(payload).build();
    }

    /**
     * Cree une nouvelle commande.
     *
     * @param body Corps de la requete contenant les items
     * @return Reponse HTTP 201 avec la commande creee
     * @throws Exception En cas d'erreur lors de la creation
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
                plantLookup
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
     * Met a jour le statut d'une commande.
     * Requiert un utilisateur administrateur.
     *
     * @param id ID de la commande
     * @param body Corps contenant le nouveau statut
     * @return Reponse HTTP avec la commande mise a jour
     * @throws Exception En cas d'erreur lors de la mise a jour
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
            plantLookup
        );
        return Response.ok(ApiMapper.toOrder(updated, items)).build();
    }

    /**
     * Supprime une commande.
     * Requiert un utilisateur administrateur.
     *
     * @param id ID de la commande a supprimer
     * @return Reponse HTTP 200
     * @throws Exception En cas d'erreur lors de la suppression
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
        Plant plant = plantLookup.reserveStock(plantId, quantity);
        OrderItem item = new OrderItem(orderId, plantId, quantity, plant.price);
        itemRepo.create(item);

        return plant.price.multiply(BigDecimal.valueOf(quantity));
    }
}
