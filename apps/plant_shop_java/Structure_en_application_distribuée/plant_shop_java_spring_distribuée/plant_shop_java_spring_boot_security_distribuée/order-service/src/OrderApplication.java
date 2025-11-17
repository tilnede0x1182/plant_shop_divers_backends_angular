package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import util.ServiceLauncher;

@SpringBootApplication(
    scanBasePackages = {"controllers", "repository", "model", "order.security", "util"},
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class
    }
)
public class OrderApplication {

    public static void main(String[] args) {
        ServiceLauncher.launch(OrderApplication.class, "ORDER_SERVICE_PORT", 6103, "OrderService", args);
    }
}
