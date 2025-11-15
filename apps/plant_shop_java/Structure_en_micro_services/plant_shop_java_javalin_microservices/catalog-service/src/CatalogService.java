import controller.ApplicationController;
import io.javalin.Javalin;
import util.JavalinJsonMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.*;

public final class CatalogService {

    public static void main(String[] args) throws Exception {
        Config config = Config.load();
        Connection db = DriverManager.getConnection(
            config.get("DATABASE_URL"),
            config.get("DATABASE_USER"),
            config.get("DATABASE_PASS")
        );

        ApplicationController appController = new ApplicationController(db);

        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.jsonMapper(new JavalinJsonMapper());
            javalinConfig.http.defaultContentType = "application/json";
            javalinConfig.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowHost("http://localhost:5173", "http://localhost:4100");
                    it.allowCredentials = true;
                });
            });
            javalinConfig.router.apiBuilder(() -> {
                appController.getRoutes().addEndpoints();
            });
        });

        int port = Integer.parseInt(config.getOrDefault("CATALOG_SERVICE_PORT", "6102"));
        app.start(port);
        System.out.printf("🌱 CatalogService démarré sur http://localhost:%d%n", port);
    }

    static final class Config {
        private final Map<String, String> values;

        private Config(Map<String, String> values) {
            this.values = values;
        }

        public static Config load() throws IOException {
            Map<String, String> values = new HashMap<>();
            readEnv(Path.of("../config/.env"), values);
            readEnv(Path.of("config/.env"), values);
            readEnv(Path.of(".env"), values);
            return new Config(values);
        }

        public String get(String key) {
            return values.get(key);
        }

        public String getOrDefault(String key, String defaultValue) {
            return values.getOrDefault(key, defaultValue);
        }

        private static void readEnv(Path path, Map<String, String> values) throws IOException {
            if (!Files.exists(path)) {
                return;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
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
            }
        }
    }
}
