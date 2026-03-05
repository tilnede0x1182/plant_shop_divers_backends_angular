package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import util.ServiceLauncher;

@SpringBootApplication(
    scanBasePackages = {"controllers", "repository", "security", "model", "util"},
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
/**
 * Application Spring Boot pour le service d'authentification.
 */
public class AuthApplication {

    /**
     * Point d'entrée principal.
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.launch(AuthApplication.class, "AUTH_SERVICE_PORT", 6101, "AuthService", args);
    }
}
