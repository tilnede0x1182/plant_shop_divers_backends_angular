package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import util.ServiceLauncher;

@SpringBootApplication(
    scanBasePackages = {
        "controllers",
        "security",
        "repository",
        "model",
        "auth.util",
        "util"
    }
)
@EnableJpaRepositories(basePackages = "repository")
@EntityScan(basePackages = "model")
/**
 * Application Spring Boot Hibernate pour le service d'authentification.
 */
public class AuthApplication {

    private static final String DB_NAME = "plant_shop_java_spring_boot_security_hibernate_distribuee";

    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.launch(AuthApplication.class, "AUTH_SERVICE_PORT", 6101, "AuthService", DB_NAME, args);
    }
}
