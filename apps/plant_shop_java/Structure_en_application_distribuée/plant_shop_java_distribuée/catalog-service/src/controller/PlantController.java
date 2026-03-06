import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;
import util.AuthContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Routes HTTP pour le service catalogue.
 */
final class CatalogRoutes implements HttpHandler {

    private final PlantController controller;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	public CatalogRoutes(Connection db) {
        this.controller = new PlantController(db);
    }

    /**
     * Traite une requête HTTP entrante.
     * @param ex L'échange HTTP
     */
    @Override
    public void handle(HttpExchange ex) throws IOException {
        controller.handle(ex);
    }
}

/**
 * Contrôleur de base avec méthodes utilitaires.
 */
abstract class CatalogBaseController {
    protected final Connection db;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	CatalogBaseController(Connection db) {
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
 * Contrôleur pour la gestion des plantes.
 */
public final class PlantController extends CatalogBaseController {

    private final PlantRepository repo;
    private final Comparator<Plant> nameComparator;

    /**
	 * Constructeur avec connexion base de données.
	 * @param db Connexion à la base de données
	 */
	public PlantController(Connection db) {
        super(db);
        this.repo = new PlantRepository(db);
        java.text.Collator col = java.text.Collator.getInstance(Locale.ROOT);
        col.setStrength(java.text.Collator.PRIMARY);
        this.nameComparator = (a, b) -> col.compare(a.name, b.name);
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
            if (path.startsWith("/admin/plants")) {
                if (!ctx.isAuthenticated() || !ctx.isAdmin()) {
                    sendJson(ex, 403, "{\"error\":\"Accès administrateur requis\"}");
                    return;
                }
                handleAdmin(ex, method, path);
                return;
            }

            if (path.startsWith("/plants")) {
                handlePublic(ex, method, path, ctx);
                return;
            }

            sendJson(ex, 404, "{\"error\":\"Route inconnue\"}");
        } catch (Exception e) {
            handleException(ex, e);
        }
    }

    /**
	 * Gère les routes publiques.
	 * @param ex L'échange HTTP
	 * @param method La méthode HTTP
	 * @param path Le chemin de la requête
	 * @param ctx Le contexte d'authentification
	 */
	private void handlePublic(HttpExchange ex, String method, String path, AuthContext ctx) throws Exception {
        if ("GET".equals(method)) {
            int id = extractId(path);
            if (id == -1) {
                list(ex);
            } else {
                show(ex, id);
            }
            return;
        }
        sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
    }

    /**
	 * Gère les routes administrateur.
	 * @param ex L'échange HTTP
	 * @param method La méthode HTTP
	 * @param path Le chemin de la requête
	 */
	private void handleAdmin(HttpExchange ex, String method, String path) throws Exception {
        int id = extractId(path);
        if ("GET".equals(method)) {
            if (id == -1) {
                list(ex);
            } else {
                show(ex, id);
            }
            return;
        }
        if ("POST".equals(method) && id == -1) {
            create(ex);
            return;
        }
        if ("PATCH".equals(method) && id != -1) {
            update(ex, id);
            return;
        }
        if ("DELETE".equals(method) && id != -1) {
            destroy(ex, id);
            return;
        }
        sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
    }

    /**
	 * Extrait l'identifiant depuis le chemin.
	 * @param path Le chemin de la requête
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
	 * Liste toutes les plantes.
	 * @param ex L'échange HTTP
	 */
	private void list(HttpExchange ex) throws Exception {
        List<Plant> plants = repo.list();
        plants.sort(nameComparator);
        JSONArray array = new JSONArray();
        for (Plant plant : plants) {
            array.put(plant.toJson());
        }
        sendJson(ex, 200, array);
    }

    /**
	 * Affiche une plante par son identifiant.
	 * @param ex L'échange HTTP
	 * @param id L'identifiant de la plante
	 */
	private void show(HttpExchange ex, int id) throws Exception {
        Plant plant = repo.find(id);
        if (plant == null) {
            sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
            return;
        }
        sendJson(ex, 200, plant.toJson());
    }

    /**
	 * Crée une nouvelle plante.
	 * @param ex L'échange HTTP
	 */
	private void create(HttpExchange ex) throws Exception {
        JSONObject body = parseJson(ex);
        String name = body.optString("name", null);
        BigDecimal price = body.has("price") ? body.getBigDecimal("price") : null;
        if (name == null || price == null) {
            sendJson(ex, 400, "{\"error\":\"name et price sont requis\"}");
            return;
        }

        Plant plant = new Plant(
            0,
            name,
            body.optString("description", ""),
            price,
            body.optInt("stock", 0),
            null
        );
        int id = repo.create(plant);
        plant.id = id;
        sendJson(ex, 201, plant.toJson());
    }

    /**
	 * Met à jour une plante.
	 * @param ex L'échange HTTP
	 * @param id L'identifiant de la plante
	 */
	private void update(HttpExchange ex, int id) throws Exception {
        Plant existing = repo.find(id);
        if (existing == null) {
            sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
            return;
        }
        JSONObject body = parseJson(ex);
        if (body.has("name")) existing.name = body.getString("name");
        if (body.has("description")) existing.description = body.getString("description");
        if (body.has("price")) existing.price = body.getBigDecimal("price");
        if (body.has("stock")) existing.stock = body.getInt("stock");
        repo.update(existing);
        sendJson(ex, 200, existing.toJson());
    }

    /**
	 * Supprime une plante.
	 * @param ex L'échange HTTP
	 * @param id L'identifiant de la plante
	 */
	private void destroy(HttpExchange ex, int id) throws Exception {
        repo.delete(id);
        sendEmpty(ex, 200);
    }
}
