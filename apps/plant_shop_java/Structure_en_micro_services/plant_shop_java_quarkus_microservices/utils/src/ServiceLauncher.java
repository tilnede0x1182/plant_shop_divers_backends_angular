package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lanceur de services Quarkus.
 * Localise le fast-jar et demarre le service sur le port configure.
 */
public final class ServiceLauncher {

    private static final String ENV_FILE = "config/.env";

    /** Constructeur prive pour empecher l'instanciation. */
    private ServiceLauncher() {}

    /**
     * Lance un service Quarkus.
     *
     * @param serviceName Nom du service
     * @param envPortKey Cle de la variable d'environnement pour le port
     * @param defaultPort Port par defaut
     * @param args Arguments de ligne de commande
     */
    public static void run(String serviceName, String envPortKey, int defaultPort, String[] args) {
        Path projectRoot = locateProjectRoot();
        Map<String, String> env = loadEnv(projectRoot.resolve(ENV_FILE));
        String port = env.getOrDefault(envPortKey, String.valueOf(defaultPort));

        Path jar = projectRoot.resolve(".quarkus")
                               .resolve(serviceName)
                               .resolve("quarkus-app")
                               .resolve("quarkus-run.jar");
        if (!Files.exists(jar)) {
            System.err.printf("❌ %s introuvable. Lancez `make compile` pour générer le bundle.%n", jar);
            System.exit(1);
        }

        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Djava.util.logging.manager=org.jboss.logmanager.LogManager");
        command.add("-Dquarkus.http.port=" + port);
        command.add("-jar");
        command.add(jar.toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectRoot.toFile());
        pb.inheritIO();
        try {
            Process process = pb.start();
            int exit = process.waitFor();
            System.exit(exit);
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Impossible de lancer le fast-jar : " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Localise la racine du projet.
     *
     * @return Chemin de la racine
     */
    private static Path locateProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.exists(current.resolve("Makefile")) && Files.exists(current.resolve(".quarkus"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Impossible de localiser le projet (Makefile/.quarkus).");
    }

    /**
     * Charge les variables d'environnement depuis un fichier.
     *
     * @param path Chemin du fichier .env
     * @return Map des variables
     */
    private static Map<String, String> loadEnv(Path path) {
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(path)) {
            return values;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
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
            System.err.println("⚠️  Impossible de lire " + path + " : " + e.getMessage());
        }
        values.putAll(System.getenv());
        return values;
    }
}
