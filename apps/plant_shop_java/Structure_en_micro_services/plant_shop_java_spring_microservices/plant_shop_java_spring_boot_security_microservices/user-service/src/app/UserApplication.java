package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import util.ServiceLauncher;

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
/** Application Spring Boot pour le service utilisateur */
public class UserApplication {

    /** Point d'entree du service utilisateur */
    public static void main(String[] args) {
        ServiceLauncher.launch(UserApplication.class, "USER_SERVICE_PORT", 6104, "UserService", args);
    }
}
