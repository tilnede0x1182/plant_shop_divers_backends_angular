package controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.sql.Connection;
import util.Response;

/**
 * Dispatcher central de l'application.
 * Inspiré par les systèmes de routage de frameworks comme Rails/Laravel.
 * Toutes les requêtes commençant par /api/ sont dirigées ici.
 * La classe analyse le chemin et délègue au contrôleur approprié.
 */
public final class Routes implements HttpHandler {

    private final AuthController authController;
    private final UserController userController;
    private final PlantController plantController;
    private final OrderController orderController;
    // OrderItemController n'est plus nécessaire ici car ses routes sont des sous-routes de /orders
    // et sont gérées directement par OrderController pour plus de simplicité.

    public Routes(Connection db) {
        this.authController = new AuthController(db);
        this.userController = new UserController(db);
        this.plantController = new PlantController(db);
        this.orderController = new OrderController(db);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        // Le chemin de la requête, ex: /users/5, /auth/login
        String path = ex.getRequestURI().getPath().substring("/api".length());

        try {
            // Délégation au contrôleur approprié en fonction du début du chemin.
            if (path.startsWith("/auth")) {
                authController.handle(ex);
            } else if (path.startsWith("/users")) {
                userController.handle(ex);
            } else if (path.startsWith("/plants") || path.startsWith("/admin/plants")) {
                // Le PlantController gère les deux types de routes
                plantController.handle(ex);
            } else if (path.startsWith("/orders")) {
                orderController.handle(ex);
            } else if (path.startsWith("/admin/users")) {
                 // Pourrait être géré par un AdminUserController dédié ou par UserController avec des vérifications de rôle
                userController.handle(ex);
            } else {
                Response.send(ex, 404, "{\"error\":\"Route API non trouvée\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Réponse d'erreur générique en cas d'exception non interceptée dans un contrôleur.
            Response.send(ex, 500, "{\"error\":\"Erreur interne du serveur\"}");
        }
    }
}
