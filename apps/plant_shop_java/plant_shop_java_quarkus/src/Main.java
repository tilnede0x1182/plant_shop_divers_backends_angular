import controllers.AuthController;
import controllers.OrderController;
import controllers.PlantController;
import controllers.UserController;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import repositories.OrderItemRepository;
import repositories.OrderRepository;
import repositories.PlantRepository;
import repositories.UserRepository;
import security.AuthenticatedUser;
import security.CorsConfig;
import security.Guards;
import security.SessionAuthFilter;
import security.SessionService;
import utils.DatabaseFactory;
import utils.QuarkusBootstrap;

/**
 * Point d'entrée principal de l'application.
 */
public class Main {

    public static void main(String[] args) {
        Weld weld = new Weld()
            .disableDiscovery()
            .beanClasses(
                QuarkusBootstrap.class,
                DatabaseFactory.class,
                SessionService.class,
                AuthenticatedUser.class,
                Guards.class,
                SessionAuthFilter.class,
                CorsConfig.class,
                AuthController.class,
                PlantController.class,
                UserController.class,
                OrderController.class,
                UserRepository.class,
                PlantRepository.class,
                OrderRepository.class,
                OrderItemRepository.class
            );

        try (WeldContainer container = weld.initialize()) {
            container.select(QuarkusBootstrap.class).get().run(args);
        } catch (Exception e) {
            System.err.println("❌ Erreur au démarrage : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
