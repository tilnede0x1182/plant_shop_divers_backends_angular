package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import util.ServiceLauncher;

@SpringBootApplication(
    scanBasePackages = {"controllers", "repository", "model", "catalog.security", "util"},
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class
    }
)
public class CatalogApplication {

    public static void main(String[] args) {
        ServiceLauncher.launch(CatalogApplication.class, "CATALOG_SERVICE_PORT", 6102, "CatalogService", args);
    }
}
