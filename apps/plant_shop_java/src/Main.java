import com.sun.net.httpserver.*;
import java.sql.Connection;
import controller.UserController;
import controller.PlantController;
import controller.OrderController;
import controller.OrderItemController;

/**
 * Un seul **dispatcher** : toutes les requêtes arrivent ici
 * puis sont déléguées au bon contrôleur en fonction du chemin.
 *
 * Avantage : compatibilité Java 1.6 (pas de lambdas, pas de regex dans HttpServer).
 */
public final class Routes implements HttpHandler {

    private final UserController       users;
    private final PlantController      plants;
    private final OrderController      orders;
    private final OrderItemController  orderItems;

    public Routes(Connection db) {
        users      = new UserController(db);
        plants     = new PlantController(db);
        orders     = new OrderController(db);
        orderItems = new OrderItemController(db);
    }

    /** Enregistrement sur le serveur */
    public static void mount(HttpServer server, Connection db) {
        server.createContext("/", new Routes(db));              // racine catch-all
    }

    /** Dispatcher rudimentaire */
    public void handle(HttpExchange ex) {
        String path = ex.getRequestURI().getPath();             // ex: /users/5
        try {
            if (path.startsWith("/users")) {
                users.handle(ex);         return;
            }
            if (path.startsWith("/plants")) {
                plants.handle(ex);        return;
            }
            if (path.startsWith("/orders/") && path.endsWith("/items")) {
                orderItems.handle(ex);    return;
            }
            if (path.startsWith("/orders")) {
                orders.handle(ex);        return;
            }
            // inconnue
            byte[] b = "{\"error\":\"Not Found\"}".getBytes("UTF-8");
            ex.getResponseHeaders().add("Content-Type","application/json");
            ex.sendResponseHeaders(404, b.length);
            ex.getResponseBody().write(b);
            ex.close();
        } catch (Exception e) {
            try {
                e.printStackTrace();
                byte[] b = ("{\"error\":\""+e.getMessage()+"\"}").getBytes("UTF-8");
                ex.getResponseHeaders().add("Content-Type","application/json");
                ex.sendResponseHeaders(500, b.length);
                ex.getResponseBody().write(b);
                ex.close();
            } catch (Exception ignore) {}
        }
    }
}
