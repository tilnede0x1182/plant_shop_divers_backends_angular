package catalog.controller;

import catalog.model.Plant;
import catalog.repository.PlantRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import util.AuthContext;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.io.BufferedReader;

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
            if (path.startsWith("/admin/plants")) {
                if (!ctx.isAdmin()) {
                    sendJson(ex, 403, "{\"error\":\"Accès administrateur requis\"}");
                    return;
                }

                if ("POST".equals(method)) {
                    create(ex, ctx);
                } else if ("PUT".equals(method)) {
                    update(ex, ctx);
                } else {
                    sendJson(ex, 405, "{\"error\":\"Méthode non autorisée\"}");
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

    private void create(HttpExchange ex, AuthContext ctx) throws Exception {
        JSONObject body = parseJson(ex);
        Plant plant = Plant.fromJson(body);
        repo.create(plant);
        sendJson(ex, 201, plant.toJson());
    }

    private void update(HttpExchange ex, AuthContext ctx) throws Exception {
        JSONObject body = parseJson(ex);
        Plant plant = Plant.fromJson(body);
        if (plant.id() <= 0) {
            throw new IllegalArgumentException("L'ID de la plante est requis pour la mise à jour");
        }
        repo.update(plant);
        sendJson(ex, 200, plant.toJson());
    }

    private void getAll(HttpExchange ex, AuthContext ctx) throws Exception {
        var plants = repo.findAll();
        String json = plants.stream()
            .map(Plant::toJson)
            .map(JSONObject::toString) // Correction pour l'erreur de collect
            .collect(Collectors.joining(",", "[", "]"));
        sendJson(ex, 200, json);
    }

    private void getOne(HttpExchange ex, AuthContext ctx) throws Exception {
        try {
            int id = Integer.parseInt(ex.getRequestURI().getPath().substring("/plants/".length()));
            Plant plant = repo.find(id);
            if (plant != null) {
                sendJson(ex, 200, plant.toJson());
            } else {
                sendJson(ex, 404, "{\"error\":\"Plante introuvable\"}");
            }
        } catch (NumberFormatException e) {
            sendJson(ex, 400, "{\"error\":\"ID invalide\"}");
        }
    }
}
