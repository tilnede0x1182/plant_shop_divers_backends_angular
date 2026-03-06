package order;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import order.controller.OrderController;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

/**
 * Service de gestion des commandes.
 */
public final class OrderService {

    private static Connection db;
    private static HttpServer server;
    /**
	 * Point d'entrée principal.
	 * @param args Arguments de ligne de commande
	 * @throws Exception En cas d'erreur au démarrage
	 */
	public static void main(String[] args) throws Exception {
        Map<String, String> cfg = loadEnv();
        int port = Integer.parseInt(cfg.getOrDefault("ORDER_SERVICE_PORT", "6103"));
        String url = cfg.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/plant_shop_java_microservices");
        String user = cfg.getOrDefault("DATABASE_USER", "postgres");
        String pass = cfg.getOrDefault("DATABASE_PASS", "postgres");

        db = DriverManager.getConnection(url, user, pass);
        server = HttpServer.create(new InetSocketAddress(port), 0);

        String catalogUrl = "http://localhost:" + cfg.getOrDefault("CATALOG_SERVICE_PORT", "6102");
        OrderController controller = new OrderController(db, catalogUrl);

        server.createContext("/", controller::handle);
        server.setExecutor(null);
        server.start();
        System.out.println("OrderService démarré sur le port " + port);
    }

    /**
	 * Charge les variables d'environnement depuis les fichiers .env.
	 * @return Map des variables d'environnement
	 * @throws IOException En cas d'erreur de lecture
	 */
	private static Map<String, String> loadEnv() throws IOException {
        Map<String, String> values = new HashMap<>();
        readEnv(Path.of("../config/.env"), values);
        readEnv(Path.of(".env"), values);
        return values;
    }

    /**
	 * Lit un fichier .env et remplit la map de valeurs.
	 * @param path Chemin du fichier
	 * @param values Map à remplir
	 * @throws IOException En cas d'erreur de lecture
	 */
	private static void readEnv(Path path, Map<String, String> values) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
            }
        }
    }
}
