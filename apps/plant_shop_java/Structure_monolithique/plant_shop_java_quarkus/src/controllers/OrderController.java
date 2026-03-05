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
import utils.ApiMapper;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
/**
 * Contrôleur REST pour la gestion des commandes.
 * Permet de lister, créer, modifier et supprimer des commandes.
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
     * Liste les commandes de l'utilisateur connecté.
     * @return 200 avec la liste des commandes, 401 si non connecté
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
     * @param body Contient items avec plantId et quantity
     * @return 201 avec la commande créée, 400 si données invalides
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
     * Modifie le statut d'une commande (admin uniquement).
     * @param id ID de la commande
     * @param body Contient status
     * @return 200 avec la commande modifiée, 404 si non trouvée
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
     * Supprime une commande (admin uniquement).
     * @param id ID de la commande
     * @return 200 OK
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
     *
     * @param orderId int Identifiant de la commande
     * @param itemMap Map<String,Integer> Map contenant plantId et quantity
     * @return BigDecimal Prix total de l'item
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
