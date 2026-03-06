package util;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
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
 * Fournit la connexion JDBC via CDI.
 * Crée une nouvelle connexion pour chaque requête HTTP (@RequestScoped).
 */
@ApplicationScoped
public final class DatabaseFactory {

    private static final Path ENV_FILE = Path.of("config", ".env");
    private static Map<String, String> envCache;

    /**
     * Charge les variables d'environnement depuis le fichier .env.
     *
     * @return Map des variables d'environnement
     * @throws IOException En cas d'erreur de lecture
     */
    private synchronized Map<String, String> loadEnv() throws IOException {
        if (envCache == null) {
            // Tentative de charger depuis la config du service courant (config/.env)
            if (!Files.exists(ENV_FILE)) {
                // Tentative de charger depuis le répertoire parent (../config/.env)
                Path parentEnv = Path.of("..", "config", ".env");
                if (Files.exists(parentEnv)) {
                    loadEnvFromFile(parentEnv);
                    return envCache;
                }
                envCache = Map.of();
                return envCache;
            }
            loadEnvFromFile(ENV_FILE);
        }
        return envCache;
    }

    /**
     * Charge les variables depuis un fichier specifique.
     *
     * @param file Chemin du fichier .env
     * @throws IOException En cas d'erreur de lecture
     */
    private void loadEnvFromFile(Path file) throws IOException {
        Map<String, String> values = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    values.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                }
            }
        }
        envCache = values;
    }

    /**
     * Produit une connexion JDBC pour la requete courante.
     *
     * @return Connexion a la base de donnees
     * @throws SQLException En cas d'erreur de connexion
     * @throws IOException En cas d'erreur de lecture de la config
     */
    @Produces
    @RequestScoped
    public Connection connection() throws SQLException, IOException {
        Map<String, String> env = loadEnv();
        String url = env.get("DATABASE_URL");
        String user = env.get("DATABASE_USER");
        String pass = env.get("DATABASE_PASS");
        if (url == null || user == null || pass == null) {
            throw new IllegalStateException("DATABASE_URL, DATABASE_USER et DATABASE_PASS doivent être définis dans config/.env");
        }
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Ferme une connexion en fin de requete.
     *
     * @param connection Connexion a fermer
     */
    public void closeConnection(@Disposes Connection connection) {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("⚠️  Erreur lors de la fermeture de la connexion : " + e.getMessage());
            }
        }
    }
}
