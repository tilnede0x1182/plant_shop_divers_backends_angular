import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        Map<String, String> env = loadEnv();
        int port = parsePort(env.getOrDefault("SERVER_ADDRESS", "4100"));

        Path jarPath = Path.of(".quarkus", "quarkus-app", "quarkus-run.jar");
        if (!Files.exists(jarPath)) {
            System.err.println("❌ quarkus-run.jar introuvable. Lancez `make compile` au préalable.");
            System.exit(1);
        }

        if (!isPortAvailable(port)) {
            System.err.printf("❌ Le port %d est déjà utilisé. Impossible de démarrer Quarkus.%n", port);
            System.exit(1);
        }

        runFastJar(jarPath, port);
    }

    private static Map<String, String> loadEnv() {
        Path envPath = Path.of("config", ".env");
        if (!Files.exists(envPath)) {
            return new HashMap<>();
        }
        Map<String, String> values = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
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

    private static int parsePort(String raw) {
        try {
            if (raw.contains(":")) {
                String[] parts = raw.split(":");
                return Integer.parseInt(parts[parts.length - 1].trim());
            }
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            System.err.printf("⚠️  Valeur de port invalide (%s), utilisation de 4100.%n", raw);
            return 4100;
        }
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void runFastJar(Path jarPath, int port) {
        ProcessBuilder pb = new ProcessBuilder(
            "java",
            "-Djava.util.logging.manager=org.jboss.logmanager.LogManager",
            "-Dquarkus.http.port=" + port,
            "-jar",
            jarPath.toString()
        );
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exit = process.waitFor();
            if (exit != 0) {
                System.err.println("❌ Le serveur Quarkus a quitté avec le code " + exit);
                System.exit(exit);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Impossible de lancer Quarkus : " + e.getMessage());
            System.exit(1);
        }
    }
}
