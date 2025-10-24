package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import org.json.JSONObject;

/**
 * Routeur central – délègue aux contrôleurs spécialisés.
 * Protège automatiquement les routes /api/admin/* par vérification de l'admin
 * via le cookie session_id déjà implémenté dans getAuthenticatedUser().
 */
public final class Routes implements HttpHandler {

	private final AuthController   auth;
	private final PlantController  plants;
	// Ajoutez d’autres contrôleurs si nécessaire

	public Routes(Connection db) {
		this.auth   = new AuthController(db);
		this.plants = new PlantController(db);
	}

	@Override
	public void handle(HttpExchange ex) throws IOException {
		String path   = ex.getRequestURI().getPath().substring("/api".length()); // sans /api
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

			/* ---------- 404 ---------- */
			sendJson404(ex, "Route non trouvée");

		} catch (Exception e) {
			e.printStackTrace();
			sendJson500(ex, e.getMessage());
		}
	}

	/* ---------- Helpers réponses JSON ---------- */
	private static void sendJson404(HttpExchange ex, String msg) throws IOException {
		byte[] b = ("{\"error\":\"" + msg + "\"}").getBytes("UTF-8");
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		ex.sendResponseHeaders(404, b.length);
		ex.getResponseBody().write(b);
		ex.close();
	}

	private static void sendJson500(HttpExchange ex, String msg) throws IOException {
		byte[] b = ("{\"error\":\"Erreur interne du serveur: " + msg + "\"}").getBytes("UTF-8");
		ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
		ex.sendResponseHeaders(500, b.length);
		ex.getResponseBody().write(b);
		ex.close();
	}
}
