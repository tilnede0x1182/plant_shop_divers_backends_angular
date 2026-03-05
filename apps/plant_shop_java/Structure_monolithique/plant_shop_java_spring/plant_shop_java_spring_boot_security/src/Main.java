import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe principale de l application Spring Boot Security.
 * Configure le serveur pour utiliser un port personnalise depuis .env et demarre l application.
 */
@SpringBootApplication(
    scanBasePackages = {"controllers", "repositories", "security", "utils"},
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
public class Main {

    private static final Path ENV_FILE = Path.of("config", ".env");

    /**
     * Point d entree de l application.
     *
     * @param args String[] Arguments de la ligne de commande
     * @throws Exception Exception En cas d erreur de demarrage
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
        System.out.println("🚀 Lancement du serveur Spring Boot Security sur http://localhost:" + port);

        SpringApplication app = new SpringApplication(Main.class);
        app.addListeners((ApplicationListener<ApplicationReadyEvent>) event ->
            System.out.println("✅ Serveur Spring prêt sur http://localhost:" + port)
        );
        app.run(args);
    }

    /**
     * Charge les variables d environnement depuis le fichier .env.
     *
     * @return Map<String, String> Variables d environnement chargees
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
     * Parse la valeur brute du port depuis le fichier .env.
     *
     * @param rawPort String Valeur brute du port
     * @return int Port parse ou 4100 par defaut
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
     * Verifie si un port est disponible.
     *
     * @param port int Port a verifier
     * @return boolean true si le port est disponible, false sinon
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
