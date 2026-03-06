package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import util.ServiceLauncher;

/**
 * Application Spring Boot pour le service catalogue.
 * Gère les opérations CRUD sur les plantes.
 */
@SpringBootApplication(
    scanBasePackages = {
        "controllers",
        "repository",
        "security",
        "model",
        "models",
        "util"
    },
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class
    }
)
public class CatalogApplication {

    /**
     * Point d'entrée de l'application CatalogService.
     * @param args Arguments de la ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.launch(CatalogApplication.class, "CATALOG_SERVICE_PORT", 6102, "CatalogService", args);
    }
}
