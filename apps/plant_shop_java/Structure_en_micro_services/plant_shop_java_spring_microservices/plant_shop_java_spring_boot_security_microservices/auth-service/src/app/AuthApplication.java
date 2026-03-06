package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import util.ServiceLauncher;

/**
 * Application Spring Boot pour le service d'authentification.
 * Gère l'inscription, la connexion et la gestion des sessions utilisateurs.
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
        HibernateJpaAutoConfiguration.class
    }
)
public class AuthApplication {

    /**
     * Point d'entrée de l'application AuthService.
     * @param args Arguments de la ligne de commande
     */
    public static void main(String[] args) {
        ServiceLauncher.launch(AuthApplication.class, "AUTH_SERVICE_PORT", 6101, "AuthService", args);
    }
}
