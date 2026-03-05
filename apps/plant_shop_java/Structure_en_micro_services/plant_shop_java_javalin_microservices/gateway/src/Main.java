package gateway.core;

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
 * Point d'entrée principal de la gateway.
 */
public final class Main {

    private final GatewayConfig config;
    private final HttpClient http;
    /**
     * Constructeur privé.
     * @param config Configuration de la gateway
     * @param http Client HTTP
     */
    private Main(GatewayConfig config, HttpClient http) {
        this.config = config;
        this.http = http;
    }

    /**
     * Crée une instance de Main avec la configuration.
     * @return Instance configurée
     */
    public static Main create() throws Exception {
        GatewayConfig config = GatewayConfig.load();
        HttpClient http = HttpClient.newBuilder().build();
        return new Main(config, http);
    }

    /**
     * Démarre le serveur HTTP de la gateway.
     */
    public void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(config.port()), 0);
        server.createContext("/api", new GatewayHandler(config, http));
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(16));
        server.start();
        System.out.printf("🚪 Gateway en écoute sur http://localhost:%d/api%n", config.port());
    }
}

/**
 * Configuration de la gateway.
 */
final class GatewayConfig {
    private final Map<String, String> values;
    /**
     * Constructeur privé.
     * @param values Valeurs de configuration
     */
    private GatewayConfig(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Charge la configuration depuis les fichiers .env.
     * @return Configuration chargée
     */
    public static GatewayConfig load() throws IOException {
        Map<String, String> values = new HashMap<>();
        readEnv(Path.of("../config/.env"), values);
        readEnv(Path.of(".env"), values);
        return new GatewayConfig(values);
    }

    /**
     * Retourne le port de la gateway.
     * @return Port configuré
     */
    public int port() {
        return Integer.parseInt(values.getOrDefault("SERVER_ADDRESS", "4100"));
    }

    /**
     * Retourne l'URL d'un service par son nom.
     * @param service Nom du service
     * @return URL du service
     */
    public String serviceUrl(String service) {
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
     * Vérifie si une route nécessite une authentification.
     * @param service Service cible
     * @param method Méthode HTTP
     * @param path Chemin de la route
     * @return true si auth requise
     */
    public boolean requiresAuth(String service, String method, String path) {
        if ("auth".equals(service)) {
            return false;
        }
        if ("catalog".equals(service) && "GET".equals(method) && path.startsWith("/plants")
            && !path.startsWith("/admin")) {
            return false;
        }
        return true;
    }

    /**
     * Lit un fichier .env et ajoute les valeurs à la map.
     * @param path Chemin du fichier
     * @param values Map à remplir
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
