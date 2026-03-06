package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import util.ServiceLauncher;

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
/**
 * Application Spring Boot du service User.
 * Gère les utilisateurs et leur profil.
 */
public class UserApplication {

    private static final String DB_NAME = "plant_shop_java_spring_boot_security_hibernate_microservices";

    /**
     * Point d'entrée du service.
     * @param args String[] Arguments CLI
     */
    public static void main(String[] args) {
        ServiceLauncher.launch(UserApplication.class, "USER_SERVICE_PORT", 6104,
            "UserService", DB_NAME, args);
    }
}
