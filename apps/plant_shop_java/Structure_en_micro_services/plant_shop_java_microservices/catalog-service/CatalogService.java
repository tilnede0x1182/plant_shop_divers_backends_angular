import catalog.controller.PlantController;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service catalogue autonome.
 */
public final class CatalogService {

    private static Connection db;
    private static HttpServer server;
    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     * @throws Exception En cas d'erreur au démarrage
     */
    public static void main(String[] args) throws Exception {
        Map<String, String> cfg = loadEnv();
        int port = Integer.parseInt(cfg.getOrDefault("CATALOG_SERVICE_PORT", "6102"));
        String url = cfg.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost:5432/plant_shop_java_microservices");
        String user = cfg.get("DATABASE_USER");
        String pass = cfg.get("DATABASE_PASS");
        if (user == null || pass == null) {
            throw new IllegalStateException("DATABASE_USER et DATABASE_PASS doivent être définis.");
        }

        db = DriverManager.getConnection(url, user, pass);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        PlantController controller = new PlantController(db);

        // MODIFIÉ : Cast en HttpHandler pour résoudre l'erreur
        server.createContext("/", (com.sun.net.httpserver.HttpHandler) controller::handle);

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();
        System.out.printf("🌱 CatalogService disponible sur http://localhost:%d%n", port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (server != null) {
                server.stop(0);
            }
            if (db != null) {
                try {
                    db.close();
                } catch (SQLException ignored) {}
            }
        }));
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
