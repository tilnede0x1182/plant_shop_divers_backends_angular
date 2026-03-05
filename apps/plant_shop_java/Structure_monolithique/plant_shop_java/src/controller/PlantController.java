package controller;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import model.Plant;
import model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.PlantRepository;

/**
 * Contrôleur pour la gestion des plantes.
 * Gère les opérations CRUD sur les plantes.
 */
public final class PlantController extends BaseController {

    private final PlantRepository repo;

    public PlantController(Connection db) {
        super(db);
        this.repo = new PlantRepository(db);
    }

    /**
     * Gère les requêtes HTTP pour les plantes.
     * @param ex HttpExchange Échange HTTP
     * @throws IOException En cas d'erreur I/O
     */
    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            User currentUser = getAuthenticatedUser(ex);
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            String[] seg = path.split("/");
            boolean isAdminRoute = path.startsWith("/api/admin/plants");

						int id = -1;
						/* Accepte /api/plants/{id} et /api/admin/plants/{id}
							Le dernier segment est l’ID, quelle que soit la profondeur. */
						if (seg.length >= 4) {
								String last = seg[seg.length - 1];
								try { id = Integer.parseInt(last); } catch (NumberFormatException ignore) {}
						}

            if ("GET".equals(method)) {
                if (id != -1) show(ex, id);
                else list(ex, isAdminRoute, currentUser);
            } else if ("POST".equals(method) && id == -1 && isAdminRoute) {
                create(ex, currentUser);
            } else if ("PATCH".equals(method) && id != -1 && isAdminRoute) {
                update(ex, currentUser, id);
            } else if ("DELETE".equals(method) && id != -1 && isAdminRoute) {
                destroy(ex, currentUser, id);
            } else {
                // Si la route admin est appelée avec une méthode non-admin
                if (isAdminRoute) {
                     sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
                } else {
                     sendJsonResponse(ex, 404, "{\"error\":\"Not Found\"}");
                }
            }
        } catch (Exception e) {
            handleError(ex, e);
        }
    }

    /**
     * Liste toutes les plantes.
     * @param ex HttpExchange Échange HTTP
     * @param isAdminRoute boolean Route admin ou non
     * @param currentUser User Utilisateur connecté
     * @throws Exception En cas d'erreur
     */
    private void list(HttpExchange ex, boolean isAdminRoute, User currentUser) throws Exception {
        // La route /admin/plants est aussi gérée par ce contrôleur
        if (isAdminRoute && (currentUser == null || !currentUser.isAdmin)) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
				/* tri alphabétique croissant, sensible aux accents comme localeCompare JS */
				java.text.Collator col = java.text.Collator.getInstance(java.util.Locale.ROOT);
				col.setStrength(java.text.Collator.TERTIARY);          // casse + accents pris en compte

				List<Plant> all = repo.list();
				all.sort((p1, p2) -> col.compare(p1.name, p2.name));

				org.json.JSONArray jsonArray = new org.json.JSONArray();
				for (Plant p : all) {
						jsonArray.put(toJson(p));
				}
				sendJsonResponse(ex, 200, jsonArray.toString());
    }

    /**
     * Affiche le détail d'une plante.
     * @param ex HttpExchange Échange HTTP
     * @param id int Identifiant de la plante
     * @throws Exception En cas d'erreur
     */
    private void show(HttpExchange ex, int id) throws Exception {
        Plant p = repo.find(id);
        if (p == null) {
            sendJsonResponse(ex, 404, "{\"error\":\"Not Found\"}");
            return;
        }
        sendJsonResponse(ex, 200, toJson(p).toString());
    }

    /**
     * Crée une nouvelle plante.
     * @param ex HttpExchange Échange HTTP
     * @param currentUser User Utilisateur connecté
     * @throws Exception En cas d'erreur
     */
    private void create(HttpExchange ex, User currentUser) throws Exception {
        if (currentUser == null || !currentUser.isAdmin) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        JSONObject body = parseJsonBody(ex);
        String name = body.optString("name", null);
        BigDecimal price = body.optBigDecimal("price", null);
        if (name == null || price == null) {
            sendJsonResponse(ex, 400, "{\"error\":\"name & price requis\"}");
            return;
        }
        String desc = body.optString("description", null);
        int stock = body.optInt("stock", 0);

        Plant newPlant = new Plant(name, desc, price, stock);
        int id = repo.create(newPlant);
        newPlant.id = id;
        sendJsonResponse(ex, 201, toJson(newPlant).toString());
    }

    /**
     * Met à jour une plante existante.
     * @param ex HttpExchange Échange HTTP
     * @param currentUser User Utilisateur connecté
     * @param id int Identifiant de la plante
     * @throws Exception En cas d'erreur
     */
    private void update(HttpExchange ex, User currentUser, int id) throws Exception {
        if (currentUser == null || !currentUser.isAdmin) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        Plant p = repo.find(id);
        if (p == null) {
            sendJsonResponse(ex, 404, "{\"error\":\"Not Found\"}");
            return;
        }

        JSONObject body = parseJsonBody(ex);
        if (body.has("name")) p.name = body.getString("name");
        if (body.has("description")) p.description = body.getString("description");
        if (body.has("price")) p.price = body.getBigDecimal("price");
        if (body.has("stock")) p.stock = body.getInt("stock");

        repo.update(p);
        sendJsonResponse(ex, 200, toJson(p).toString());
    }

    /**
     * Supprime une plante.
     * @param ex HttpExchange Échange HTTP
     * @param currentUser User Utilisateur connecté
     * @param id int Identifiant de la plante
     * @throws Exception En cas d'erreur
     */
    private void destroy(HttpExchange ex, User currentUser, int id) throws Exception {
        if (currentUser == null || !currentUser.isAdmin) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        repo.delete(id);
        sendEmptyResponse(ex, 200); // CORRIGÉ: Le test attend 200, pas 204.
    }

    /**
     * Convertit une plante en JSON.
     * @param p Plant Plante à convertir
     * @return JSONObject Objet JSON
     */
    private JSONObject toJson(Plant p) {
        JSONObject json = new JSONObject();
        json.put("id", p.id);
        json.put("name", p.name);
        json.put("description", p.description != null ? p.description : JSONObject.NULL);
        json.put("price", p.price);
        json.put("stock", p.stock);
        if (p.createdAt != null) {
            json.put("createdAt", p.createdAt.toInstant().toString());
        }
        return json;
    }
}
