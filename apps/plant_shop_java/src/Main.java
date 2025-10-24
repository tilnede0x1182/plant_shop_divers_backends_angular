import com.sun.net.httpserver.HttpServer;
import controller.Routes; // Importation de notre nouveau routeur
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Point d'entrée de l'application.
 *  - Charge la configuration depuis .env
 *  - Ouvre la connexion à la base de données (JDBC)
 *  - Crée le serveur HTTP
 *  - Monte le routeur principal sur le chemin /api
 *  - Démarre le serveur
 */
public final class Main {

    private static Map<String, String> env() throws IOException {
        Map<String, String> m = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
            String l;
            while ((l = br.readLine()) != null) {
                int i = l.indexOf('=');
                if (i > 0) {
                    m.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Attention: Fichier .env non trouvé. Utilisation des valeurs par défaut.");
        }
        return m;
    }

    public static void main(String[] args) throws Exception {

        /* ---------- Configuration ---------- */
        Map<String, String> cfg = env();
        String dbUrl = cfg.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost/plant_shop_java");
        String dbUser = cfg.get("DATABASE_USER");
        String dbPass = cfg.get("DATABASE_PASS");
        int port = Integer.parseInt(cfg.getOrDefault("SERVER_ADDRESS", "4100"));

        if (dbUser == null || dbPass == null) {
            throw new IllegalStateException("Les variables DATABASE_USER et DATABASE_PASS sont manquantes dans le fichier .env");
        }

				/* ---------- Connexion JDBC ---------- */
				Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPass);

				HttpServer server = null;
				try {
					server = HttpServer.create(new InetSocketAddress(port), 0);
					server.createContext("/api", new Routes(db));
					server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
					server.start();

					System.out.println("🚀 Serveur démarré sur http://localhost:" + port);
					System.out.println("   Routes API disponibles sur http://localhost:" + port + "/api");
				} catch (java.net.BindException e) {
					System.err.println("❌ Erreur : Le port " + port + " est déjà utilisé. Un autre serveur est peut-être en cours d'exécution.");
					if (db != null && !db.isClosed()) db.close();
					System.exit(1);
				} catch (Exception e) {
					System.err.println("❌ Erreur lors du démarrage du serveur : " + e.getMessage());
					if (db != null && !db.isClosed()) db.close();
					System.exit(2);
				}

				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					try {
						if (db != null && !db.isClosed()) {
							db.close();
							System.out.println("Connexion à la base de données fermée.");
						}
					} catch (Exception ignore) {}
					if (server != null) {
						server.stop(0);
						System.out.println("Serveur HTTP arrêté.");
					}
				}));
		}
}
