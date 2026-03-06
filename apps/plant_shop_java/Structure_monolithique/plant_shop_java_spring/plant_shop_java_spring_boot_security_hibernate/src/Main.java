import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationListener;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe principale de l'application Spring Boot.
 */
@SpringBootApplication(scanBasePackages = {"controllers", "repositories", "security", "utils", "models"})
@EnableJpaRepositories(basePackages = "repositories")
@EntityScan(basePackages = "models")
public class Main {

    private static final Path ENV_FILE = Path.of("config", ".env");

    /**
     * Point d'entrée de l'application Spring Boot.
     * @param args String[] Arguments de ligne de commande
     * @throws Exception En cas d'erreur au démarrage
     */
    public static void main(String[] args) throws Exception {
        Map<String, String> env = loadEnv();
        int port = parsePort(env.getOrDefault("SERVER_ADDRESS",
            env.getOrDefault("SERVER_ADRRESS", "4100")));

        if (!isPortAvailable(port)) {
            System.err.println("❌ Port " + port + " déjà utilisé, arrêt du lancement Spring Boot Security.");
            return;
        }

        System.setProperty("server.port", String.valueOf(port));
        applyDataSourceOverrides(env);
        System.out.println("🚀 Lancement du serveur Spring Boot Security sur http://localhost:" + port);

        SpringApplication app = new SpringApplication(Main.class);
        app.addListeners((ApplicationListener<ApplicationReadyEvent>) event ->
            System.out.println("✅ Serveur Spring prêt sur http://localhost:" + port)
        );
        app.run(args);
    }

    /**
     * Applique les surcharges de configuration de la base de données.
     * @param env Map<String,String> Variables d'environnement
     */
    private static void applyDataSourceOverrides(Map<String, String> env) {
        setIfPresent(env, "DATABASE_URL", "spring.datasource.url");
        setIfPresent(env, "DATABASE_USER", "spring.datasource.username");
        setIfPresent(env, "DATABASE_PASS", "spring.datasource.password");
    }

    /**
     * Définit une propriété système si la clé existe dans l'environnement.
     * @param env Map<String,String> Variables d'environnement
     * @param envKey String Clé dans l'environnement
     * @param propertyKey String Clé de propriété système
     */
    private static void setIfPresent(Map<String, String> env, String envKey, String propertyKey) {
        String value = env.get(envKey);
        if (value != null && !value.isBlank()) {
            System.setProperty(propertyKey, value);
        }
    }

    /**
     * Charge les variables d'environnement depuis config/.env.
     * @return Map<String,String> Variables chargées
     */
    private static Map<String, String> loadEnv() {
        if (!Files.exists(ENV_FILE)) {
            return Map.of();
        }
        Map<String, String> values = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(ENV_FILE)) {
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
        } catch (IOException e) {
            System.err.println("⚠️  Impossible de lire config/.env : " + e.getMessage());
        }
        return values;
    }

    /**
     * Parse le port depuis une chaîne.
     * @param rawPort String Valeur brute du port
     * @return int Port parsé (4100 par défaut)
     */
    private static int parsePort(String rawPort) {
        if (rawPort == null || rawPort.isBlank()) {
            return 4100;
        }
        String candidate = rawPort.contains(":") ? rawPort.substring(rawPort.indexOf(':') + 1) : rawPort;
        try {
            return Integer.parseInt(candidate.trim());
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Valeur de port invalide (" + rawPort + "), utilisation de 4100.");
            return 4100;
        }
    }

    /**
     * Vérifie si un port est disponible.
     * @param port int Port à vérifier
     * @return boolean true si disponible
     */
    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
