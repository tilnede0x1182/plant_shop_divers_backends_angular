package catalog.controller;

import catalog.model.Plant;
import catalog.repository.PlantRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.stream.Collectors;
import org.json.JSONObject;
import util.AuthContext;

// Définition locale de BaseController pour résoudre les dépendances du classpath lors de la compilation
abstract class BaseController implements HttpHandler {
    protected final Connection db;

    BaseController(Connection db) {
        this.db = db;
    }

    protected JSONObject parseJson(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return body.isBlank() ? new JSONObject() : new JSONObject(body);
    }

    // Définition de sendJson pour JSONObject
    protected void sendJson(HttpExchange ex, int code, JSONObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    // Définition de sendJson pour String
    protected void sendJson(HttpExchange ex, int code, String jsonBody) throws IOException {
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
        ex.close();
    }

    protected void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }

    // Méthode abstraite/interface pour le handler
    @Override
    public abstract void handle(HttpExchange exchange) throws IOException;
}

public final class PlantController extends BaseController {

    private static final String ADMIN_BASE = "/admin/plants";

    private final PlantRepository repo;

    public PlantController(Connection db) {
        super(db);
        this.repo = new PlantRepository(db);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();
        AuthContext ctx = AuthContext.fromHeaders(ex);

        try {
            // Route interne pour la mise à jour du stock
            if (path.startsWith("/internal/plants") && "PATCH".equals(method)) {
                handleInternalStockUpdate(ex, path);
                return;
            }

            // Routes admin
            if (path.startsWith(ADMIN_BASE)) {
                if (!ctx.isAdmin()) {
                    sendJson(ex, 403, "{\"error\":\"Accès administrateur requis\"}");
                    return;
                }

                if (ADMIN_BASE.equals(path)) {
                    switch (method) {
                        case "POST" -> create(ex);
                        case "GET" -> listAdmin(ex);
                        default -> sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
                    }
                    return;
                }

                int id = extractPlantId(path, ADMIN_BASE + "/");
                if (id <= 0) {
                    sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
                    return;
                }

                switch (method) {
                    case "GET" -> sendPlant(ex, id);
                    case "PUT" -> replace(ex, id);
                    case "PATCH" -> patch(ex, id);
                    case "DELETE" -> delete(ex, id);
                    default -> sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
                }
                return;
            }

            // Routes publiques
            if ("/plants".equals(path) && "GET".equals(method)) {
                getAll(ex, ctx);
                return;
            }
            if (path.startsWith("/plants/") && "GET".equals(method)) {
                getOne(ex, ctx);
                return;
            }

            sendJson(ex, 404, "{\"error\":\"Route non trouvée\"}");

        } catch (IllegalArgumentException e) {
            sendJson(ex, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, "{\"error\":\"Erreur interne du serveur\"}");
        }
    }

    /**
     * Gère la mise à jour du stock pour les appels internes.
     * Route : PATCH /internal/plants/:id/stock
     */
    private void handleInternalStockUpdate(HttpExchange ex, String path) throws Exception {
        String[] parts = path.split("/");
        int id = -1;
        if (parts.length == 5 && "stock".equals(parts[4])) {
            try {
                id = Integer.parseInt(parts[3]);
            } catch (NumberFormatException ignored) {}
        }

        if (id == -1) {
            sendJson(ex, 400, "{\"error\":\"ID de plante manquant ou format URL incorrect\"}");
            return;
        }

        Plant existing = repo.find(id);
        if (existing == null) {
            sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
            return;
        }

        JSONObject body = parseJson(ex);
        if (!body.has("stock")) {
            sendJson(ex, 400, "{\"error\":\"Le champ 'stock' est requis\"}");
            return;
        }

        Plant updated = existing.withStock(body.getInt("stock"));
        repo.update(updated);

        sendJson(ex, 200, repo.find(id).toJson());
    }

    private int extractPlantId(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return -1;
        }
        try {
            return Integer.parseInt(path.substring(prefix.length()));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void create(HttpExchange ex) throws Exception {
        JSONObject body = parseJson(ex);
        Plant plant = Plant.fromJson(body);
        int id = repo.create(plant);
        Plant created = repo.find(id);
        if (created == null) {
            created = plant.withId(id);
        }
        sendJson(ex, 201, created.toJson());
    }

    private void replace(HttpExchange ex, int id) throws Exception {
        JSONObject body = parseJson(ex);
        Plant plant = Plant.fromJson(body).withId(id);
        repo.update(plant);
        Plant updated = repo.find(id);
        sendJson(ex, 200, (updated != null ? updated : plant).toJson());
    }

    private void patch(HttpExchange ex, int id) throws Exception {
        Plant existing = repo.find(id);
        if (existing == null) {
            sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
            return;
        }

        JSONObject body = parseJson(ex);
        Plant patched = applyPatch(existing, body);
        repo.update(patched);
        Plant updated = repo.find(id);
        sendJson(ex, 200, (updated != null ? updated : patched).toJson());
    }

    private void delete(HttpExchange ex, int id) throws Exception {
        Plant existing = repo.find(id);
        if (existing == null) {
            sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
            return;
        }
        repo.delete(id);
        sendEmpty(ex, 200);
    }

    private void listAdmin(HttpExchange ex) throws Exception {
        sendPlantList(ex);
    }

    private void getAll(HttpExchange ex, AuthContext ctx) throws Exception {
        sendPlantList(ex);
    }

    private void getOne(HttpExchange ex, AuthContext ctx) throws Exception {
        try {
            int id = Integer.parseInt(ex.getRequestURI().getPath().substring("/plants/".length()));
            sendPlant(ex, id);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, "{\"error\":\"ID invalide\"}");
        }
    }

    private void sendPlant(HttpExchange ex, int id) throws Exception {
        Plant plant = repo.find(id);
        if (plant != null) {
            sendJson(ex, 200, plant.toJson());
        } else {
            sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
        }
    }

    private Plant applyPatch(Plant base, JSONObject body) {
        Plant current = base;
        if (body.has("name")) {
            if (body.isNull("name")) {
                throw new IllegalArgumentException("Le champ 'name' ne peut pas être nul");
            }
            current = current.withName(body.getString("name"));
        }
        if (body.has("description")) {
            current = current.withDescription(body.isNull("description") ? null : body.getString("description"));
        }
        if (body.has("price")) {
            if (body.isNull("price")) {
                throw new IllegalArgumentException("Le champ 'price' ne peut pas être nul");
            }
            current = current.withPrice(body.getBigDecimal("price"));
        }
        if (body.has("stock")) {
            if (body.isNull("stock")) {
                throw new IllegalArgumentException("Le champ 'stock' ne peut pas être nul");
            }
            current = current.withStock(body.getInt("stock"));
        }
        return current;
    }

    private void sendPlantList(HttpExchange ex) throws Exception {
        var plants = repo.findAllOrderedByName();
        String json = plants.stream()
            .map(Plant::toJson)
            .map(JSONObject::toString)
            .collect(Collectors.joining(",", "[", "]"));
        sendJson(ex, 200, json);
    }
}
