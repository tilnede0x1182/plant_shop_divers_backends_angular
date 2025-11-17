package app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import util.ServiceLauncher;

@SpringBootApplication(
    scanBasePackages = {"controllers", "repository", "model", "security", "util"},
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
public class UserApplication {

    public static void main(String[] args) {
        ServiceLauncher.launch(UserApplication.class, "USER_SERVICE_PORT", 6104, "UserService", args);
    }
}
