package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import util.ServiceLauncher;

/**
 * Application Spring Boot du service Catalog.
 */
@SpringBootApplication(
    scanBasePackages = {
        "controllers",
        "repository",
        "model",
        "security",
        "util"
    }
)
@EnableJpaRepositories(basePackages = "repository")
@EntityScan(basePackages = "model")
/** Application Spring Boot du service Catalog. */
public class CatalogApplication {

    private static final String DB_NAME = "plant_shop_java_spring_boot_security_hibernate_microservices";

    /**
     * Point d'entrée du service.
     * @param args String[] Arguments CLI
     */
    public static void main(String[] args) {
        ServiceLauncher.launch(CatalogApplication.class, "CATALOG_SERVICE_PORT", 6102,
            "CatalogService", DB_NAME, args);
    }
}
