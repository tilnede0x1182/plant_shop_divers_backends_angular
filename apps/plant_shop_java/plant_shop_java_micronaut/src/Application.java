import io.micronaut.runtime.Micronaut;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

public class Application {

    public static void main(String[] args) {
        try {
            Map<String, String> env = loadEnv();

            // Lecture du port depuis .env (clé SERVER_ADDRESS)
            int port = parsePort(env.getOrDefault("SERVER_ADDRESS", "4100"));

            if (!isPortAvailable(port)) {
                System.err.println("❌ Le port " + port + " est déjà utilisé. Impossible de démarrer le serveur.");
                System.exit(1);
            }

						Micronaut.build(args)
										.banner(false)
										.properties(Map.of("micronaut.server.port", String.valueOf(port)))
										.start();

            System.out.println("🚀 Serveur Micronaut démarré sur http://localhost:" + port);

        } catch (Exception e) {
            System.err.println("❌ Erreur au démarrage : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Map<String, String> loadEnv() {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("config/.env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                int i = line.indexOf('=');
                if (i > 0) {
                    map.put(line.substring(0, i).trim(), line.substring(i + 1).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️  Fichier config/.env introuvable, utilisation des valeurs par défaut.");
        }
        return map;
    }

    private static int parsePort(String value) {
        try {
            if (value.contains(":")) {
                return Integer.parseInt(value.split(":")[1]);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Valeur SERVER_ADDRESS invalide, utilisation du port 4100.");
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
}
