package controller;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import model.Order;
import model.OrderItem;
import model.Plant;
import model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.OrderItemRepository;
import repository.OrderRepository;
import repository.PlantRepository;

/**
 * Contrôleur gérant les commandes (CRUD).
 */
public final class OrderController extends BaseController {

    private final OrderRepository repo;
    private final OrderItemRepository itemRepo;
    private final PlantRepository plantRepo; // AJOUTÉ: Pour récupérer les infos des plantes

    /**
     * Constructeur du contrôleur de commandes.
     * @param db Connection Connexion à la base de données
     */
    public OrderController(Connection db) {
        super(db);
        this.repo = new OrderRepository(db);
        this.itemRepo = new OrderItemRepository(db);
        this.plantRepo = new PlantRepository(db); // AJOUTÉ
    }

    /**
     * Dispatche les requêtes vers les méthodes CRUD.
     * @param ex HttpExchange Échange HTTP
     * @throws IOException En cas d'erreur I/O
     */
    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            User currentUser = getAuthenticatedUser(ex);
            if (currentUser == null) {
                sendJsonResponse(ex, 401, "{\"error\":\"Authentification requise\"}");
                return;
            }

            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            String[] seg = path.split("/");

            int id = -1;
            if (seg.length == 4) { // /api/orders/{id}
                id = Integer.parseInt(seg[3]);
            }

            if ("GET".equals(method)) {
                if (id != -1) show(ex, currentUser, id);
                else list(ex, currentUser);
            } else if ("POST".equals(method) && id == -1) {
                create(ex, currentUser);
            } else if ("PATCH".equals(method) && id != -1) {
                patch(ex, currentUser, id);
            } else if ("DELETE".equals(method) && id != -1) {
                destroy(ex, currentUser, id);
            } else {
                sendJsonResponse(ex, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            handleError(ex, e);
        }
    }

    /**
     * Liste les commandes de l'utilisateur.
     * @param ex HttpExchange Échange HTTP
     * @param currentUser User Utilisateur connecté
     * @throws Exception En cas d'erreur
     */
    private void list(HttpExchange ex, User currentUser) throws Exception {
        List<Order> all = repo.list();
        /* ordre décroissant : dernière commande en premier */
        all.sort((o1, o2) -> o2.createdAt.compareTo(o1.createdAt));

        JSONArray array = new JSONArray();
        for (Order o : all) {
                // Montrer uniquement les commandes appartenant à l'utilisateur courant.
                if (o.userId == currentUser.id) {
                        array.put(toJson(o, null));
                }
        }
        sendJsonResponse(ex, 200, array.toString());
    }

    /**
     * Affiche une commande par ID.
     * @param ex HttpExchange Échange HTTP
     * @param currentUser User Utilisateur connecté
     * @param id int ID de la commande
     * @throws Exception En cas d'erreur
     */
    private void show(HttpExchange ex, User currentUser, int id) throws Exception {
        Order o = repo.find(id);
        if (o == null || (o.userId != currentUser.id && !currentUser.isAdmin)) {
            sendJsonResponse(ex, 404, "{\"error\":\"Not Found\"}");
            return;
        }
        List<OrderItem> items = itemRepo.listByOrder(id);
        sendJsonResponse(ex, 200, toJson(o, items).toString());
    }

    /**
     * Crée une nouvelle commande.
     * @param ex HttpExchange Échange HTTP
     * @param currentUser User Utilisateur connecté
     * @throws Exception En cas d'erreur
     */
    private void create(HttpExchange ex, User currentUser) throws Exception {
        JSONObject body = parseJsonBody(ex);
        JSONArray itemsJson = body.optJSONArray("items");
        if (itemsJson == null || itemsJson.isEmpty()) {
            sendJsonResponse(ex, 400, "{\"error\":\"items requis\"}");
            return;
        }

        // CORRIGÉ: Utiliser l'ID de l'utilisateur authentifié
        int userId = currentUser.id;
        BigDecimal total = BigDecimal.ZERO;
        int orderId = repo.create(new Order(userId, total, "pending"));

        for (int i = 0; i < itemsJson.length(); i++) {
            JSONObject it = itemsJson.getJSONObject(i);
            int plantId = it.getInt("plantId");
            int quantity = it.getInt("quantity");

            Plant p = plantRepo.find(plantId);
            if (p == null) throw new Exception("Plante avec id " + plantId + " non trouvée.");

            BigDecimal price = p.price; // Utiliser le prix actuel de la plante
            total = total.add(price.multiply(new BigDecimal(quantity)));
            // CORRIGÉ: Appel à `create` au lieu de `addItem`
            itemRepo.create(new OrderItem(orderId, plantId, quantity, price));
        }
        repo.updateTotal(orderId, total);
        Order newOrder = repo.find(orderId);
        sendJsonResponse(ex, 201, toJson(newOrder, null).toString());
    }

    /**
     * Met à jour le statut d'une commande (admin).
     * @param ex HttpExchange Échange HTTP
     * @param currentUser User Utilisateur connecté
     * @param id int ID de la commande
     * @throws Exception En cas d'erreur
     */
    private void patch(HttpExchange ex, User currentUser, int id) throws Exception {
        if (!currentUser.isAdmin) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        Order o = repo.find(id);
        if (o == null) {
            sendJsonResponse(ex, 404, "{\"error\":\"Not Found\"}");
            return;
        }

        JSONObject body = parseJsonBody(ex);
        String status = body.optString("status", null);
        if (status != null) {
            // CORRIGÉ: Utilisation de la méthode du repository
            repo.updateStatus(id, status);
            o.status = status; // Mettre à jour l'objet local pour la réponse
        }
        sendJsonResponse(ex, 200, toJson(o, null).toString());
    }

    /**
     * Supprime une commande (admin).
     * @param ex HttpExchange Échange HTTP
     * @param currentUser User Utilisateur connecté
     * @param id int ID de la commande
     * @throws Exception En cas d'erreur
     */
    private void destroy(HttpExchange ex, User currentUser, int id) throws Exception {
        if (!currentUser.isAdmin) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        itemRepo.deleteByOrder(id);
        repo.delete(id);
        sendEmptyResponse(ex, 200); // CORRIGÉ: Le test attend 200
    }

		/**
		 * Convertit une commande en JSONObject.
		 * @param o Order Commande à convertir
		 * @param items List<OrderItem> Items de la commande
		 * @return JSONObject Représentation JSON
		 * @throws Exception En cas d'erreur
		 */
		private JSONObject toJson(Order o, List<OrderItem> items) throws Exception {
				/* Charge toujours les items si absents */
				if (items == null) {
						items = itemRepo.listByOrder(o.id);
				}

				JSONObject json = new JSONObject();
				json.put("id", o.id);
				json.put("userId", o.userId);
				json.put("total", o.total);
				json.put("status", o.status);
				json.put("createdAt", o.createdAt.toInstant().toString());

				/* Tableau orderItems toujours présent, même lors du listing */
				JSONArray itemsArray = new JSONArray();
				for (OrderItem it : items) {
						Plant p = plantRepo.find(it.plantId);
						if (p == null) {
								continue;
						}

						JSONObject itemJson = new JSONObject();
						itemJson.put("id", it.id);
						itemJson.put("plantId", it.plantId);
						itemJson.put("quantity", it.quantity);
						itemJson.put("price", it.price);

						JSONObject plantJson = new JSONObject();
						plantJson.put("id", p.id);
						plantJson.put("name", p.name);
						plantJson.put("price", p.price);
						itemJson.put("plant", plantJson);

						itemsArray.put(itemJson);
				}
				json.put("orderItems", itemsArray);

				return json;
		}
}
