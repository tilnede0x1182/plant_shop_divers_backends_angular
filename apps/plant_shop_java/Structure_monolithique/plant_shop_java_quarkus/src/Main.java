import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import utils.QuarkusBundleBuilder;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        try {
            QuarkusBundleBuilder.buildBundle();
        } catch (Exception e) {
            System.err.println("❌ Échec de la génération du bundle Quarkus : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        Map<String, String> env = loadEnv();
        int port = parsePort(env.getOrDefault("SERVER_ADDRESS", "4100"));

        if (!isPortAvailable(port)) {
            System.err.printf("❌ Le port %d est déjà utilisé. Impossible de démarrer Quarkus.%n", port);
            System.exit(1);
        }

        runFastJar(port);
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

    private static void runFastJar(int port) {
        Path jarPath = Path.of(".quarkus", "quarkus-app", "quarkus-run.jar");
        if (!Files.exists(jarPath)) {
            System.err.println("❌ quarkus-run.jar introuvable dans " + jarPath.toAbsolutePath());
            System.exit(1);
            return;
        }

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
        command.add("-Dquarkus.http.port=" + port);
        command.add("-jar");
        command.add(jarPath.toString());

        ProcessBuilder pb = new ProcessBuilder(command);
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
