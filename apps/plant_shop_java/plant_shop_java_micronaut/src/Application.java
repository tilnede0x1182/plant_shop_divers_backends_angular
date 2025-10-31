import io.micronaut.context.annotation.Factory;
import io.micronaut.runtime.Micronaut;
import jakarta.inject.Singleton;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

    @Factory
    public static class DatabaseConfiguration {

        private static Map<String, String> env() throws IOException {
            Map<String, String> m = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader("config/.env"))) {
                String l;
                while ((l = br.readLine()) != null) {
                    int i = l.indexOf('=');
                    if (i > 0) m.put(l.substring(0, i).trim(), l.substring(i + 1).trim());
                }
            } catch (IOException e) {
                System.err.println("Attention: Fichier .env non trouvé.");
            }
            return m;
        }

        @Singleton
        public Connection connection() throws SQLException, IOException {
            Map<String, String> cfg = env();
            String dbUrl = cfg.getOrDefault("DATABASE_URL", "jdbc:postgresql://localhost/plant_shop_micronaut");
            String dbUser = cfg.get("DATABASE_USER");
            String dbPass = cfg.get("DATABASE_PASS");

            if (dbUser == null || dbPass == null) {
                throw new IllegalStateException("DATABASE_USER et DATABASE_PASS sont requis dans config/.env");
            }

            return DriverManager.getConnection(dbUrl, dbUser, dbPass);
        }
    }
}
