package controller;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import model.User;
import org.json.JSONArray;
import org.json.JSONObject;
import repository.UserRepository;
import util.PasswordUtil;

public final class UserController extends BaseController {

    private final UserRepository repo;

    public UserController(Connection db) {
        super(db);
        this.repo = new UserRepository(db);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            User currentUser = getAuthenticatedUser(ex);

            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            String[] seg = path.split("/");
            boolean isAdminRoute = path.startsWith("/api/admin/users");

						/* ---------- extraction de l'ID, quelle que soit la profondeur ---------- */
						int id = -1;
						if (seg.length >= 4) {                       // /api/(admin/)?users/{id}
								String last = seg[seg.length - 1];       // dernier segment
								try { id = Integer.parseInt(last); } catch (NumberFormatException ignore) {}
						}

            if ("GET".equals(method)) {
                if (id != -1) show(ex, currentUser, id);
                else list(ex, currentUser);
            } else if ("POST".equals(method) && id == -1) {
                create(ex, currentUser);
            } else if ("PATCH".equals(method) && id != -1) {
                update(ex, currentUser, id);
            } else if ("DELETE".equals(method) && id != -1) {
                destroy(ex, currentUser, id);
            } else {
                sendJsonResponse(ex, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            handleError(ex, e);
        }
    }

    private void list(HttpExchange ex, User currentUser) throws Exception {
        // La route GET /users est protégée et réservée aux admins
        if (currentUser == null || !currentUser.isAdmin) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        List<User> all = repo.list();
        JSONArray jsonArray = new JSONArray();
        for (User u : all) {
            jsonArray.put(toJson(u));
        }
        sendJsonResponse(ex, 200, jsonArray.toString());
    }

    private void show(HttpExchange ex, User currentUser, int id) throws Exception {
        if (currentUser == null || (currentUser.id != id && !currentUser.isAdmin)) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        User u = repo.find(id);
        if (u == null) {
            sendJsonResponse(ex, 404, "{\"error\":\"Not Found\"}");
            return;
        }
        sendJsonResponse(ex, 200, toJson(u).toString());
    }

    private void create(HttpExchange ex, User currentUser) throws Exception {
        // La création d'utilisateur est réservée aux admins dans ce contexte de test
        if (currentUser == null || !currentUser.isAdmin) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        JSONObject body = parseJsonBody(ex);
        String name = body.optString("name", null);
        String email = body.optString("email", null);
        String pass = body.optString("password", null);
        if (name == null || email == null || pass == null) {
            sendJsonResponse(ex, 400, "{\"error\":\"Champs manquants\"}");
            return;
        }
        boolean isAdmin = body.optBoolean("admin", false);

        String hash = PasswordUtil.hashPassword(pass);
        User newUser = new User(name, email, hash, isAdmin);
        int newId = repo.create(newUser);
        newUser.id = newId; // Assigner l'ID retourné

        // CORRIGÉ: Renvoyer l'objet complet pour la cohérence
        sendJsonResponse(ex, 201, toJson(newUser).toString());
    }

    private void update(HttpExchange ex, User currentUser, int id) throws Exception {
        if (currentUser == null || (currentUser.id != id && !currentUser.isAdmin)) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        User u = repo.find(id);
        if (u == null) {
            sendJsonResponse(ex, 404, "{\"error\":\"Not Found\"}");
            return;
        }

        JSONObject body = parseJsonBody(ex);
        if (body.has("name")) u.name = body.getString("name");
        if (body.has("email")) u.email = body.getString("email");

        if (body.has("admin") && currentUser.isAdmin) {
            u.isAdmin = body.getBoolean("admin");
        }

        repo.update(u);
        sendJsonResponse(ex, 200, toJson(u).toString());
    }

    private void destroy(HttpExchange ex, User currentUser, int id) throws Exception {
        if (currentUser == null || !currentUser.isAdmin) {
            sendJsonResponse(ex, 403, "{\"error\":\"Accès interdit\"}");
            return;
        }
        repo.delete(id);
        sendEmptyResponse(ex, 200);
    }

    private JSONObject toJson(User u) {
        JSONObject json = new JSONObject();
        json.put("id", u.id);
        json.put("name", u.name);
        json.put("email", u.email);
        json.put("admin", u.isAdmin);
        if (u.createdAt != null) {
            json.put("createdAt", u.createdAt.toInstant().toString());
        }
        return json;
    }
}
