package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import util.ServiceLauncher;

/**
 * Application Spring Boot du service Order.
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
/**
 * Application Spring Boot du service Order.
 * Gère les commandes et les items de commande.
 */
@EntityScan(basePackages = "model")
public class OrderApplication {

    private static final String DB_NAME = "plant_shop_java_spring_boot_security_hibernate_microservices";

    /**
     * Point d'entrée du service.
     * @param args String[] Arguments CLI
     */
    public static void main(String[] args) {
        ServiceLauncher.launch(OrderApplication.class, "ORDER_SERVICE_PORT", 6103,
            "OrderService", DB_NAME, args);
    }
}
