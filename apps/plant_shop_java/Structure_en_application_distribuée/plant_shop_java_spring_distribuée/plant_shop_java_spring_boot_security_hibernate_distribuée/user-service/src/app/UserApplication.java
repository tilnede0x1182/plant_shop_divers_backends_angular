package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import util.ServiceLauncher;

@SpringBootApplication(
    scanBasePackages = {
        "controllers",
        "repository",
        "user.security",
        "user.util",
        "model",
        "util"
    },
    exclude = SecurityAutoConfiguration.class
)
@EnableJpaRepositories(basePackages = "repository")
@EntityScan(basePackages = "model")
public class UserApplication {

    private static final String DB_NAME = "plant_shop_java_spring_boot_security_hibernate_distribuee";

    public static void main(String[] args) {
        ServiceLauncher.launch(UserApplication.class, "USER_SERVICE_PORT", 6104, "UserService", DB_NAME, args);
    }
}
