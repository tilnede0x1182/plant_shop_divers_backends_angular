import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime de la Gateway HTTP.
 * Gere la creation et le demarrage du serveur.
 */
final class GatewayRuntime {

    private final GatewayConfig config;
    private final HttpClient http;

    /**
     * Constructeur prive.
     *
     * @param config Configuration de la Gateway
     * @param http Client HTTP
     */
    private GatewayRuntime(GatewayConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    /**
     * Cree une instance de GatewayRuntime.
     *
     * @return Nouvelle instance configuree
     * @throws Exception En cas d'erreur de chargement de la config
     */
    static GatewayRuntime create() throws Exception {
        GatewayConfig config = GatewayConfig.load();
        HttpClient http = HttpClient.newBuilder().build();
        return new GatewayRuntime(config, http);
    }

    /**
     * Demarre le serveur HTTP de la Gateway.
     *
     * @throws Exception En cas d'erreur au demarrage
     */
    void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/api", new GatewayHandler(config, http));
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(16));
        server.start();
        System.out.printf("🚪 Gateway en écoute sur http://localhost:%d/api%n", config.port());
    }
}

/**
 * Configuration de la Gateway.
 * Charge les variables d'environnement et fournit les URLs des services.
 */
final class GatewayConfig {
    private final Map<String, String> values;

    /**
     * Constructeur prive.
     *
     * @param values Map des valeurs de configuration
     */
    private GatewayConfig(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Charge la configuration depuis les fichiers .env.
     *
     * @return Configuration chargee
     * @throws IOException En cas d'erreur de lecture
     */
    static GatewayConfig load() throws IOException {
        Map<String, String> values = new HashMap<>();
        readEnv(locateProjectRoot().resolve("config/.env"), values);
        readEnv(Path.of(".env"), values);
        return new GatewayConfig(values);
    }

    /**
     * Retourne le port de la Gateway.
     *
     * @return Port HTTP
     */
    int port() {
        return Integer.parseInt(values.getOrDefault("GATEWAY_SERVICE_PORT", "4100"));
    }

    /**
     * Retourne l'URL d'un microservice.
     *
     * @param service Nom du service (auth, catalog, order, user)
     * @return URL complete du service
     */
    String serviceUrl(String service) {
        String host = values.getOrDefault("SERVICE_HOST", "http://localhost");
        return switch (service) {
            case "auth" -> host + ":" + values.getOrDefault("AUTH_SERVICE_PORT", "6101");
            case "catalog" -> host + ":" + values.getOrDefault("CATALOG_SERVICE_PORT", "6102");
            case "order" -> host + ":" + values.getOrDefault("ORDER_SERVICE_PORT", "6103");
            case "user" -> host + ":" + values.getOrDefault("USER_SERVICE_PORT", "6104");
            default -> throw new IllegalArgumentException("Service inconnu: " + service);
        };
    }

    /**
     * Determine si une route necessite une authentification.
     *
     * @param service Nom du service
     * @param method Methode HTTP
     * @param path Chemin de la requete
     * @return true si l'authentification est requise
     */
    boolean requiresAuth(String service, String method, String path) {
        if ("auth".equals(service)) {
            return false;
        }
        if ("catalog".equals(service)
            && "GET".equals(method)
            && path.startsWith("/api/plants")
            && !path.startsWith("/api/admin")) {
            return false;
        }
        return true;
    }

    /**
     * Lit un fichier .env et ajoute les valeurs a la map.
     *
     * @param path Chemin du fichier .env
     * @param values Map a remplir
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

    /**
     * Localise la racine du projet en remontant les dossiers.
     *
     * @return Chemin vers la racine du projet
     */
    private static Path locateProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("Makefile"))) {
                return current;
            }
            current = current.getParent();
        }
        return Path.of("").toAbsolutePath();
    }
}
