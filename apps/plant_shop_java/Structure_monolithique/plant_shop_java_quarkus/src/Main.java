import controllers.AuthController;
import controllers.OrderController;
import controllers.PlantController;
import controllers.UserController;
import java.util.concurrent.CountDownLatch;
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
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        Weld weld = new Weld();

        WeldContainer container = weld.initialize();
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        try {
            container.select(QuarkusBootstrap.class).get().run(args, shutdownLatch);
            shutdownLatch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erreur au démarrage : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            container.shutdown();
        }
    }
}
