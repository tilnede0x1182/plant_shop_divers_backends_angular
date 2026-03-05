package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;

/**
 * Routeur central : délègue chaque requête au contrôleur approprié.
 */
public final class Routes implements HttpHandler {

	private final AuthController   auth;
	private final PlantController  plants;
	private final UserController   users;
	private final OrderController  orders;   // ← ajouté

	/**
	 * Constructeur du routeur.
	 * @param db Connection Connexion à la base de données
	 */
	public Routes(Connection db) {
		this.auth   = new AuthController(db);
		this.plants = new PlantController(db);
		this.users  = new UserController(db);
		this.orders = new OrderController(db); // ← ajouté
	}

	/**
	 * Dispatche les requêtes vers les contrôleurs appropriés.
	 * @param ex HttpExchange Échange HTTP
	 * @throws IOException En cas d'erreur I/O
	 */
	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path   = ex.getRequestURI().getPath().substring("/api".length());
		String method = ex.getRequestMethod();

		try {
			/* ---------- AUTH ---------- */
			if (path.startsWith("/auth")) {
				auth.handle(ex);
				return;
			}

			/* ---------- PLANTS ---------- */
			if (path.startsWith("/plants") || path.startsWith("/admin/plants")) {
				plants.handle(ex);
				return;
			}

			/* ---------- USERS ---------- */
			if (path.startsWith("/users") || path.startsWith("/admin/users")) {
				users.handle(ex);
				return;
			}

			/* ---------- ORDERS ---------- */        // ← ajouté
			if (path.startsWith("/orders") || path.startsWith("/admin/orders")) {
				orders.handle(ex);
				return;
			}

			/* ---------- 404 ---------- */
			sendJson(ex, 404, "{\"error\":\"Route non trouvée\"}");

		} catch (Exception e) {
			e.printStackTrace();
			sendJson(ex, 500, "{\"error\":\"Erreur interne du serveur: " + e.getMessage() + "\"}");
		}
	}

	/**
	 * Envoie une réponse JSON.
	 * @param ex HttpExchange Échange HTTP
	 * @param code int Code HTTP
	 * @param body String Corps JSON
	 * @throws IOException En cas d erreur I/O
	 */
	private static void sendJson(HttpExchange ex, int code, String body) throws IOException {
		byte[] bytes = body.getBytes("UTF-8");
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		ex.sendResponseHeaders(code, bytes.length);
		ex.getResponseBody().write(bytes);
		ex.close();
	}
}
