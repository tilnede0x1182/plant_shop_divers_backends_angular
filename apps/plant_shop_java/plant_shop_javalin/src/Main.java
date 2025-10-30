// src/Main.java
import io.javalin.Javalin;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Set;
import controller.ApplicationController;

/**
 * Point d'entrée de l'application Javalin.
 *  - Charge la configuration depuis .env
 *  - Ouvre la connexion à la base de données (JDBC)
 *  - Crée et configure le serveur Javalin
 *  - Monte le routeur principal
 *  - Démarre le serveur
 */
public final class Main {
    private static Connection db = null;
    private static Javalin app = null;
    private static final Set<String> ALLOWED_ORIGINS = Set.of(
        "http://localhost:4200",
        "http://127.0.0.1:4200"
    );

    private static Map<String, String> env() throws IOException {
        Map<String, String> m = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("config/.env"))) {
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

    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            /* ---------- Configuration ---------- */
            Map<String, String> cfg = env();
            String dbUrl = cfg.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost/plant_shop_javalin");
            String dbUser = cfg.get("DATABASE_USER");
            String dbPass = cfg.get("DATABASE_PASS");
            int port = Integer.parseInt(cfg.getOrDefault("SERVER_ADDRESS", "4100"));

            if (dbUser == null || dbPass == null) {
                throw new IllegalStateException("Les variables DATABASE_USER et DATABASE_PASS sont manquantes dans le fichier .env");
            }

            if (!isPortAvailable(port)) {
                System.err.println("❌ Le port " + port + " est déjà utilisé. Impossible de démarrer le serveur.");
                System.exit(1);
            }

            // Connexion JDBC
            db = DriverManager.getConnection(dbUrl, dbUser, dbPass);

            // Contrôleur principal qui gère les routes et l'accès
            ApplicationController applicationController = new ApplicationController(db);

            app = Javalin.create(config -> {
                config.jsonMapper(new JavalinJsonMapper());
                config.http.defaultContentType = "application/json; charset=utf-8";
                config.router.apiBuilder(applicationController.getRoutes());
            });

            app.before(ctx -> {
                String origin = ctx.header("Origin");
                if (isAllowedOrigin(origin)) {
                    ctx.header("Access-Control-Allow-Origin", origin);
                    ctx.header("Access-Control-Allow-Credentials", "true");
                    ctx.header("Vary", "Origin");
                }
            });

            app.options("/*", ctx -> {
                String origin = ctx.header("Origin");
                if (isAllowedOrigin(origin)) {
                    ctx.header("Access-Control-Allow-Origin", origin);
                    ctx.header("Access-Control-Allow-Credentials", "true");
                    ctx.header("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
                    String requestHeaders = ctx.header("Access-Control-Request-Headers");
                    if (requestHeaders != null && !requestHeaders.isBlank()) {
                        ctx.header("Access-Control-Allow-Headers", requestHeaders);
                    } else {
                        ctx.header("Access-Control-Allow-Headers", "Content-Type");
                    }
                    ctx.header("Vary", "Origin");
                } else {
                    ctx.header("Access-Control-Allow-Headers", "Content-Type");
                }
                ctx.status(204);
            });

            // Démarrage du serveur
            app.start(port);
            System.out.println("🚀 Serveur Javalin démarré sur http://localhost:" + port);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage du serveur : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Hook pour fermer proprement les ressources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (db != null && !db.isClosed()) {
                    db.close();
                    System.out.println("Connexion à la base de données fermée.");
                }
            } catch (Exception ignore) {}
            if (app != null) {
                app.stop();
                System.out.println("Serveur Javalin arrêté.");
            }
        }));
    }

    private static boolean isAllowedOrigin(String origin) {
        return origin != null && ALLOWED_ORIGINS.contains(origin);
    }
}
