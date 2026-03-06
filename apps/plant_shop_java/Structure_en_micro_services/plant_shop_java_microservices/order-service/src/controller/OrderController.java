package order.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.stream.Collectors;
import order.model.Order;
import order.model.OrderItem;
import order.model.PlantStock;
import order.repository.OrderItemRepository;
import order.repository.OrderRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import util.AuthContext;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Contrôleur de base avec utilitaires communs.
 */
abstract class BaseController implements HttpHandler {
    protected final Connection db;

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	BaseController(Connection db) {
        this.db = db;
    }

    /**
	 * Parse le corps JSON d'une requête.
	 * @param ex Échange HTTP
	 * @return Objet JSON parsé
	 * @throws IOException En cas d'erreur de lecture
	 */
	protected JSONObject parseJson(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return body.isBlank() ? new JSONObject() : new JSONObject(body);
    }

    /**
	 * Envoie une réponse JSON.
	 * @param ex Échange HTTP
	 * @param code Code HTTP
	 * @param body Corps JSON
	 * @throws IOException En cas d'erreur I/O
	 */
	protected void sendJson(HttpExchange ex, int code, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse JSON en string.
	 * @param ex Échange HTTP
	 * @param code Code HTTP
	 * @param jsonBody Corps JSON en string
	 * @throws IOException En cas d'erreur I/O
	 */
	protected void sendJson(HttpExchange ex, int code, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    /**
	 * Envoie une réponse vide.
	 * @param ex Échange HTTP
	 * @param code Code HTTP
	 * @throws IOException En cas d'erreur I/O
	 */
	protected void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }

    @Override
    public abstract void handle(HttpExchange exchange) throws IOException;
}

/**
 * Repository pour accéder aux plantes.
 */
final class PlantRepository {
    private final Connection db;

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	public PlantRepository(Connection db) {
        this.db = db;
    }

    /**
	 * Trouve une plante par son ID.
	 * @param id ID de la plante
	 * @return PlantStock ou null
	 * @throws SQLException En cas d'erreur SQL
	 */
	public PlantStock find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM plants WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlantStock(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock")
                    );
                }
            }
        }
        return null;
    }
}

/**
 * Contrôleur des commandes.
 */
public final class OrderController extends BaseController {

    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final PlantRepository plantRepo;
    private final OrderItemController itemController;

    private final HttpClient httpClient;
    private final String catalogServiceUrl;

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 * @param catalogUrl URL du service catalogue
	 */
	public OrderController(Connection db, String catalogUrl) {
        super(db);
        this.orderRepo = new OrderRepository(db);
        this.itemRepo = new OrderItemRepository(db);
        this.plantRepo = new PlantRepository(db);
        this.itemController = new OrderItemController(db);

        this.catalogServiceUrl = catalogUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
	 * Gère les requêtes HTTP.
	 * @param ex Échange HTTP
	 * @throws IOException En cas d'erreur I/O
	 */
	@Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        AuthContext ctx = AuthContext.fromHeaders(ex);

        try {
            if (!ctx.isAuthenticated()) {
                sendJson(ex, 401, "{\"error\":\"Non authentifié\"}");
                return;
            }

            if ("/orders".equals(path) && "GET".equals(method)) {
                getAll(ex, ctx);
            } else if ("/orders".equals(path) && "POST".equals(method)) {
                create(ex, ctx);
            } else if (path.startsWith("/orders/")) {
                Integer orderId = resolveOrderId(path);
                if (orderId == null) {
                    sendJson(ex, 404, "{\"error\":\"Route non trouvée\"}");
                    return;
                }
                if ("PATCH".equals(method)) {
                    updateStatus(ex, ctx, orderId);
                } else if ("DELETE".equals(method)) {
                    deleteOrder(ex, ctx, orderId);
                } else {
                    sendJson(ex, 404, "{\"error\":\"Route non trouvée\"}");
                }
            } else {
                sendJson(ex, 404, "{\"error\":\"Route non trouvée\"}");
            }
        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, "{\"error\":\"Erreur interne du serveur\"}");
        }
    }

    /**
	 * Met à jour le stock d'une plante via le service catalogue.
	 * @param plantId ID de la plante
	 * @param newStock Nouveau stock
	 * @return true si succès
	 */
	private boolean updateCatalogStock(int plantId, int newStock) {
        try {
            JSONObject body = new JSONObject().put("stock", newStock);
            String uri = String.format("%s/internal/plants/%d/stock", this.catalogServiceUrl, plantId);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.statusCode() >= 200 && response.statusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * Crée une nouvelle commande.
	 * @param ex Échange HTTP
	 * @param ctx Contexte d'authentification
	 * @throws Exception En cas d'erreur
	 */
	private void create(HttpExchange ex, AuthContext ctx) throws Exception {
        JSONObject body = parseJson(ex);
        JSONArray items = body.getJSONArray("items");

        if (items.length() == 0) {
            throw new IllegalArgumentException("Le panier est vide");
        }

        Order order = new Order(0, ctx.userId(), BigDecimal.ZERO);
        int orderId = orderRepo.create(order);
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < items.length(); i++) {
            JSONObject obj = items.getJSONObject(i);
            int plantId = obj.getInt("plantId");
            int quantity = obj.getInt("quantity");

            if (quantity <= 0) {
                throw new IllegalArgumentException("La quantité doit être positive");
            }

            PlantStock plant = plantRepo.find(plantId);
            if (plant == null) {
                throw new IllegalArgumentException("Plante " + plantId + " introuvable");
            }
            if (plant.stock() < quantity) {
                throw new IllegalArgumentException("Stock insuffisant pour " + plant.name());
            }

            int newStock = plant.stock() - quantity;
            boolean stockUpdated = updateCatalogStock(plantId, newStock);

            if (!stockUpdated) {
                throw new RuntimeException("Echec de la mise à jour du stock pour " + plant.name());
            }

            BigDecimal price = plant.price();
            total = total.add(price.multiply(BigDecimal.valueOf(quantity)));
            itemRepo.create(new OrderItem(0, orderId, plantId, quantity, price));
        }

        orderRepo.updateTotal(orderId, total);
        Order finalOrder = orderRepo.find(orderId);

        sendJson(ex, 201, itemController.toJson(finalOrder, itemRepo.findByOrder(orderId)));
    }

    /**
	 * Récupère toutes les commandes de l'utilisateur.
	 * @param ex Échange HTTP
	 * @param ctx Contexte d'authentification
	 * @throws Exception En cas d'erreur
	 */
	private void getAll(HttpExchange ex, AuthContext ctx) throws Exception {
        var orders = orderRepo.findByUser(ctx.userId());
        String json = orders.stream()
            .map(order -> {
                try {
                    return itemController.toJson(order, itemRepo.findByOrder(order.id())).toString();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.joining(",", "[", "]"));
        sendJson(ex, 200, json);
    }

    /**
	 * Extrait l'ID de commande depuis le chemin.
	 * @param path Chemin de la requête
	 * @return ID de commande ou null
	 */
	private Integer resolveOrderId(String path) {
        String[] parts = path.split("/");
        if (parts.length < 3) {
            return null;
        }
        try {
            return Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
	 * Met à jour le statut d'une commande.
	 * @param ex Échange HTTP
	 * @param ctx Contexte d'authentification
	 * @param orderId ID de la commande
	 * @throws Exception En cas d'erreur
	 */
	private void updateStatus(HttpExchange ex, AuthContext ctx, int orderId) throws Exception {
        if (!ctx.isAdmin()) {
            sendJson(ex, 403, "{\"error\":\"Accès administrateur requis\"}");
            return;
        }
        Order order = orderRepo.find(orderId);
        if (order == null) {
            sendJson(ex, 404, "{\"error\":\"Commande introuvable\"}");
            return;
        }
        JSONObject payload = parseJson(ex);
        String status = payload.optString("status", "").trim();
        if (status.isEmpty()) {
            throw new IllegalArgumentException("Le statut est requis");
        }
        orderRepo.updateStatus(orderId, status);
        Order updated = orderRepo.find(orderId);
        var json = itemController.toJson(updated, itemRepo.findByOrder(orderId));
        sendJson(ex, 200, json);
    }

    /**
	 * Supprime une commande.
	 * @param ex Échange HTTP
	 * @param ctx Contexte d'authentification
	 * @param orderId ID de la commande
	 * @throws Exception En cas d'erreur
	 */
	private void deleteOrder(HttpExchange ex, AuthContext ctx, int orderId) throws Exception {
        if (!ctx.isAdmin()) {
            sendJson(ex, 403, "{\"error\":\"Accès administrateur requis\"}");
            return;
        }
        Order order = orderRepo.find(orderId);
        if (order == null) {
            sendJson(ex, 404, "{\"error\":\"Commande introuvable\"}");
            return;
        }
        orderRepo.remove(orderId);
        sendEmpty(ex, 200);
    }
}
