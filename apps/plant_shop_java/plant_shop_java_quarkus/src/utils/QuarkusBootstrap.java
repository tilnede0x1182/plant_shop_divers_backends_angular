package utils;

import io.quarkus.runtime.Quarkus;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

/**
 * Gère le démarrage manuel de Quarkus en lisant le port
 * depuis le fichier .env, en s'assurant qu'il est disponible,
 * puis en le passant en System Property avant de lancer Quarkus.
 */
public final class QuarkusBootstrap {

    public static void run(String[] args) throws Exception {
        Map<String, String> env = loadEnv();
        int port = parsePort(env.getOrDefault("SERVER_ADDRESS", "4100"));

        if (!isPortAvailable(port)) {
            System.err.println("❌ Le port " + port + " est déjà utilisé. Impossible de démarrer le serveur.");
            System.exit(1);
        }

        // Définir le port pour Quarkus via une System Property
        System.setProperty("quarkus.http.port", String.valueOf(port));

        System.out.println("🚀 Serveur Quarkus en démarrage sur http://localhost:" + port);

        // Démarrer Quarkus (cette méthode est bloquante)
        Quarkus.run(args);
    }

    private static Map<String, String> loadEnv() {
        Map<String, String> map = new HashMap<>();
        // Note: Le Seed.java utilise "config/.env", mais le Test.java utilise ".env"
        // Nous utilisons ".env" pour être cohérent avec le Test.java
        try (BufferedReader br = new BufferedReader(new FileReader("config/.env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                int i = line.indexOf('=');
                if (i > 0) {
                    map.put(line.substring(0, i).trim(), line.substring(i + 1).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️  Fichier .env introuvable, utilisation des valeurs par défaut.");
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
