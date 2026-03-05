import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import util.AuthContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Routes HTTP pour le service de commandes.
 */
final class OrderRoutes implements HttpHandler {

    private final OrderController controller;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	public OrderRoutes(Connection db) {
        this.controller = new OrderController(db);
    }

    @Override
    /**
	 * Traite une requête HTTP entrante.
	 * @param exchange L'échange HTTP
	 */
	public void handle(HttpExchange exchange) throws IOException {
        controller.handle(exchange);
    }
}

/**
 * Contrôleur de base avec méthodes utilitaires.
 */
abstract class OrderBaseController {
    protected final Connection db;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	OrderBaseController(Connection db) {
        this.db = db;
    }

    /**
	 * Parse le corps JSON de la requête.
	 * @param ex L'échange HTTP
	 * @return L'objet JSON parsé
	 */
	protected JSONObject parseJson(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return body.isBlank() ? new JSONObject() : new JSONObject(body);
    }

    /**
	 * Envoie une réponse JSON Object.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 * @param body Le corps JSON
	 */
	protected void sendJson(HttpExchange ex, int code, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse JSON Array.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 * @param body Le tableau JSON
	 */
	protected void sendJson(HttpExchange ex, int code, JSONArray body) throws IOException {
        byte[] bytes = body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse JSON en chaîne.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 * @param body Le corps en chaîne
	 */
	protected void sendJson(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse vide.
	 * @param ex L'échange HTTP
	 * @param code Le code HTTP
	 */
	protected void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }

    /**
	 * Gère une exception et envoie une erreur 500.
	 * @param ex L'échange HTTP
	 * @param e L'exception
	 */
	protected void handleException(HttpExchange ex, Exception e) throws IOException {
        e.printStackTrace();
        sendJson(ex, 500, new JSONObject().put("error", e.getMessage()));
    }
}

/**
 * Contrôleur pour la gestion des commandes.
 */
public final class OrderController extends OrderBaseController {

    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final PlantRepository plantRepo;
    private final OrderItemController itemController;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	public OrderController(Connection db) {
        super(db);
        this.orderRepo = new OrderRepository(db);
        this.itemRepo = new OrderItemRepository(db);
        this.plantRepo = new PlantRepository(db);
        this.itemController = new OrderItemController(plantRepo);
    }

    /**
	 * Route les requêtes vers les bonnes méthodes.
	 * @param ex L'échange HTTP
	 */
	public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        AuthContext ctx = AuthContext.fromHeaders(ex);

        try {
            if (path.startsWith("/admin/orders")) {
                if (!ctx.isAuthenticated() || !ctx.isAdmin()) {
                    sendJson(ex, 403, "{\"error\":\"Accès administrateur requis\"}");
                    return;
                }
                adminRoutes(ex, method, path);
                return;
            }

            if (!ctx.isAuthenticated()) {
                sendJson(ex, 401, "{\"error\":\"Authentification requise\"}");
                return;
            }

            if (path.startsWith("/orders")) {
                userRoutes(ex, method, path, ctx);
                return;
            }

            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
        } catch (Exception e) {
            handleException(ex, e);
        }
    }

    /**
	 * Gère les routes utilisateur.
	 * @param ex L'échange HTTP
	 * @param method La méthode HTTP
	 * @param path Le chemin
	 * @param ctx Le contexte d'authentification
	 */
	private void userRoutes(HttpExchange ex, String method, String path, AuthContext ctx) throws Exception {
        int id = extractId(path);
        if ("GET".equals(method)) {
            if (id == -1) {
                list(ex, ctx);
            } else {
                show(ex, ctx, id);
            }
            return;
        }
        if ("PATCH".equals(method) && id != -1) {
            if (!ctx.isAdmin()) {
                sendJson(ex, 403, "{\"error\":\"Accès interdit\"}");
                return;
            }
            patch(ex, id);
            return;
        }
        if ("POST".equals(method) && id == -1) {
            create(ex, ctx);
            return;
        }
        if ("DELETE".equals(method) && id != -1) {
            if (!ctx.isAdmin()) {
                sendJson(ex, 403, "{\"error\":\"Accès interdit\"}");
                return;
            }
            destroy(ex, id);
            return;
        }
        sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
    }

    /**
	 * Gère les routes administrateur.
	 * @param ex L'échange HTTP
	 * @param method La méthode HTTP
	 * @param path Le chemin
	 */
	private void adminRoutes(HttpExchange ex, String method, String path) throws Exception {
        int id = extractId(path);
        if ("PATCH".equals(method)) {
            if (id == -1) {
                sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
                return;
            }
            patch(ex, id);
            return;
        }
        if ("DELETE".equals(method)) {
            if (id == -1) {
                sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
                return;
            }
            destroy(ex, id);
            return;
        }
        if ("GET".equals(method) && id != -1) {
            showAdmin(ex, id);
            return;
        }
        sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
    }

    /**
	 * Extrait l'identifiant depuis le chemin.
	 * @param path Le chemin
	 * @return L'identifiant ou -1
	 */
	private int extractId(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3) {
            try {
                return Integer.parseInt(parts[parts.length - 1]);
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    /**
	 * Liste les commandes de l'utilisateur.
	 * @param ex L'échange HTTP
	 * @param ctx Le contexte d'authentification
	 */
	private void list(HttpExchange ex, AuthContext ctx) throws Exception {
        List<Order> orders = orderRepo.listByUser(ctx.userId());
        orders.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        JSONArray arr = new JSONArray();
        for (Order order : orders) {
            arr.put(itemController.toJson(order, itemRepo.listByOrder(order.id)));
        }
        sendJson(ex, 200, arr);
    }

    /**
	 * Affiche une commande.
	 * @param ex L'échange HTTP
	 * @param ctx Le contexte d'authentification
	 * @param id L'identifiant de la commande
	 */
	private void show(HttpExchange ex, AuthContext ctx, int id) throws Exception {
        Order order = orderRepo.find(id);
        if (order == null || order.userId != ctx.userId()) {
            sendJson(ex, 404, "{\"error\":\"Commande introuvable\"}");
            return;
        }
        sendJson(ex, 200, itemController.toJson(order, itemRepo.listByOrder(id)));
    }

    /**
	 * Affiche une commande pour l'admin.
	 * @param ex L'échange HTTP
	 * @param id L'identifiant de la commande
	 */
	private void showAdmin(HttpExchange ex, int id) throws Exception {
        Order order = orderRepo.find(id);
        if (order == null) {
            sendJson(ex, 404, "{\"error\":\"Commande introuvable\"}");
            return;
        }
        sendJson(ex, 200, itemController.toJson(order, itemRepo.listByOrder(id)));
    }

    /**
	 * Crée une nouvelle commande.
	 * @param ex L'échange HTTP
	 * @param ctx Le contexte d'authentification
	 */
	private void create(HttpExchange ex, AuthContext ctx) throws Exception {
        JSONObject body = parseJson(ex);
        JSONArray items = body.optJSONArray("items");
        if (items == null || items.isEmpty()) {
            sendJson(ex, 400, "{\"error\":\"items requis\"}");
            return;
        }

        Order order = new Order(0, ctx.userId(), BigDecimal.ZERO, "pending", java.time.Instant.now());
        int orderId = orderRepo.create(order);
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < items.length(); i++) {
            JSONObject obj = items.getJSONObject(i);
            int plantId = obj.getInt("plantId");
            int quantity = obj.getInt("quantity");
            Plant plant = plantRepo.find(plantId);
            if (plant == null) {
                throw new IllegalArgumentException("Plante " + plantId + " introuvable");
            }
            if (plant.stock < quantity) {
                throw new IllegalArgumentException("Stock insuffisant pour " + plant.name);
            }
            plantRepo.updateStock(plantId, plant.stock - quantity);
            BigDecimal price = plant.price;
            total = total.add(price.multiply(BigDecimal.valueOf(quantity)));
            itemRepo.create(new OrderItem(0, orderId, plantId, quantity, price));
        }

        orderRepo.updateTotal(orderId, total);
        Order created = orderRepo.find(orderId);
        sendJson(ex, 201, itemController.toJson(created, itemRepo.listByOrder(orderId)));
    }

    /**
	 * Met à jour une commande.
	 * @param ex L'échange HTTP
	 * @param id L'identifiant de la commande
	 */
	private void patch(HttpExchange ex, int id) throws Exception {
        Order order = orderRepo.find(id);
        if (order == null) {
            sendJson(ex, 404, "{\"error\":\"Commande introuvable\"}");
            return;
        }
        JSONObject body = parseJson(ex);
        if (body.has("status")) {
            orderRepo.updateStatus(id, body.getString("status"));
        }
        Order updated = orderRepo.find(id);
        sendJson(ex, 200, itemController.toJson(updated, itemRepo.listByOrder(id)));
    }

    /**
	 * Supprime une commande.
	 * @param ex L'échange HTTP
	 * @param id L'identifiant de la commande
	 */
	private void destroy(HttpExchange ex, int id) throws Exception {
        itemRepo.deleteByOrder(id);
        orderRepo.delete(id);
        sendEmpty(ex, 200);
    }
}
