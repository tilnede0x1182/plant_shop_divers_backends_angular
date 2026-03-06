package util;

import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.config.JavalinConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime commun pour démarrer les microservices Javalin.
 */
public final class ServiceRuntime {

    /**
     * Interface fonctionnelle pour enregistrer les routes.
     */
    @FunctionalInterface
    public interface RouteRegistrar {
        EndpointGroup build(Connection db, Map<String, String> env) throws Exception;
    }

    /**
     * Descripteur d'un service.
     * @param serviceName Nom du service
     * @param portKey Clé de la variable d'environnement pour le port
     * @param defaultPort Port par défaut
     */
    public record ServiceDescriptor(String serviceName, String portKey, int defaultPort) {
    }

    /**
     * Constructeur privé pour classe utilitaire.
     */
    private ServiceRuntime() {
    }

    /**
     * Crée un descripteur de service.
     * @param serviceName Nom du service
     * @param portKey Clé de la variable d'environnement pour le port
     * @param defaultPort Port par défaut
     * @return Descripteur de service
     */
    public static ServiceDescriptor descriptor(String serviceName, String portKey, int defaultPort) {
        return new ServiceDescriptor(serviceName, portKey, defaultPort);
    }

    /**
     * Démarre un service avec le descripteur et l'enregistreur de routes.
     * @param descriptor Descripteur du service
     * @param registrar Enregistreur de routes
     * @throws Exception En cas d'erreur de démarrage
     */
    public static void start(ServiceDescriptor descriptor, RouteRegistrar registrar) throws Exception {
        Map<String, String> env = loadEnv();
        Connection connection = connect(env);

        EndpointGroup routes = registrar.build(connection, env);
        Javalin app = null;
        boolean success = false;
        try {
            app = Javalin.create(config -> {
                configureJavalin(config);
                config.router.apiBuilder(routes);
            });
            int port = resolvePort(descriptor, env);
            app.start(port);
            System.out.printf("🚀 %s en écoute sur http://localhost:%d%n", descriptor.serviceName(), port);
            success = true;
        } finally {
            if (!success) {
                safeClose(connection);
                if (app != null) {
                    app.stop();
                }
            } else {
                Connection conn = connection;
                Javalin server = app;
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    safeClose(conn);
                    if (server != null) {
                        server.stop();
                    }
                }));
            }
        }
    }

    /**
     * Configure l'instance Javalin.
     * @param config Configuration Javalin
     */
    private static void configureJavalin(JavalinConfig config) {
        config.jsonMapper(new JavalinJsonMapper());
        config.http.defaultContentType = "application/json; charset=utf-8";
        config.bundledPlugins.enableCors(cors -> cors.addRule(rule -> {
            rule.allowCredentials = true;
            rule.allowHost("http://localhost:4200");
            rule.allowHost("http://127.0.0.1:4200");
            rule.allowHost("http://localhost:4100");
            rule.allowHost("http://localhost:5173");
        }));
    }

    /**
     * Charge les variables d'environnement.
     * @return Map des variables
     * @throws IOException En cas d'erreur de lecture
     */
    private static Map<String, String> loadEnv() throws IOException {
        Map<String, String> values = new HashMap<>(System.getenv());
        Path cwd = Path.of("").toAbsolutePath();
        readEnv(cwd.resolve("../config/.env"), values);
        readEnv(cwd.resolve("config/.env"), values);
        readEnv(cwd.resolve(".env"), values);
        return values;
    }

    /**
     * Lit un fichier .env et ajoute les valeurs à la map.
     * @param path Chemin du fichier
     * @param values Map de destination
     * @throws IOException En cas d'erreur de lecture
     */
    private static void readEnv(Path path, Map<String, String> values) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                if (!key.isEmpty()) {
                    values.put(key, value);
                }
            }
        }
    }

    /**
     * Établit la connexion à la base de données.
     * @param env Variables d'environnement
     * @return Connexion BDD
     * @throws SQLException En cas d'erreur de connexion
     */
    private static Connection connect(Map<String, String> env) throws SQLException {
        String url = env.get("DATABASE_URL");
        String user = env.getOrDefault("DATABASE_USER", "");
        String pass = env.getOrDefault("DATABASE_PASS", "");
        if (url == null) {
            throw new IllegalStateException("DATABASE_URL manquant");
        }
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Résout le port du service.
     * @param descriptor Descripteur du service
     * @param env Variables d'environnement
     * @return Port résolu
     */
    private static int resolvePort(ServiceDescriptor descriptor, Map<String, String> env) {
        String raw = env.getOrDefault(descriptor.portKey(), env.get("SERVER_ADDRESS"));
        if (raw == null) {
            return descriptor.defaultPort();
        }
        return parsePort(raw, descriptor.defaultPort());
    }

    /**
     * Parse un port depuis une chaîne.
     * @param raw Valeur brute
     * @param fallback Valeur par défaut
     * @return Port parsé
     */
    private static int parsePort(String raw, int fallback) {
        try {
            if (raw.contains(":")) {
                String[] parts = raw.split(":");
                return Integer.parseInt(parts[parts.length - 1]);
            }
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * Ferme une connexion de manière sécurisée.
     * @param connection Connexion à fermer
     */
    private static void safeClose(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
