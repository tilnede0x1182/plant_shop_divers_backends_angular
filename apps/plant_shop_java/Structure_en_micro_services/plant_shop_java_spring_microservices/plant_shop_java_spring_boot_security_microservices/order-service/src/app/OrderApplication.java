package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import util.ServiceLauncher;

/**
 * Application Spring Boot pour le service de commandes.
 * Gère la création, consultation et modification des commandes.
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
public class OrderApplication {

    /**
     * Point d'entrée de l'application OrderService.
     * @param args Arguments de la ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.launch(OrderApplication.class, "ORDER_SERVICE_PORT", 6103, "OrderService", args);
    }
}
