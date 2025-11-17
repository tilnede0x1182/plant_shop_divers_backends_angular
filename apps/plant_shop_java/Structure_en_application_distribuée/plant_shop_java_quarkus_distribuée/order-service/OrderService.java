import controllers.OrderController;
import java.util.concurrent.CountDownLatch;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import repository.OrderRepository;
import repository.OrderItemRepository;
import repository.PlantRepository;
import order.repositories.BaseRepository;
import order.security.AuthContext;
import order.security.GatewayAuthFilter;
import order.security.Guards;
import order.util.ApiMapper;
import util.DatabaseFactory;
import jakarta.ws.rs.core.Application;
import java.util.Set;
import io.undertow.Undertow;
import util.EnvLoader;

/**
 * Point d'entrée du microservice OrderService.
 * Configure Weld CDI et démarre un serveur RESTEasy/Undertow sur le port 6103.
 */
public class OrderService {

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        try {
            int port = Integer.parseInt(EnvLoader.get("ORDER_SERVICE_PORT", "6103"));

            UndertowJaxrsServer server = new UndertowJaxrsServer();
            Undertow.Builder builder = Undertow.builder().addHttpListener(port, "0.0.0.0");
            server.start(builder);

            ResteasyDeployment deployment = new ResteasyDeploymentImpl();
            deployment.setApplication(new OrderServiceApplication());
            deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
            server.deploy(deployment);

            System.out.printf("🧾 OrderService disponible sur http://localhost:%d%n", port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                shutdownLatch.countDown();
                server.stop();
            }));

            shutdownLatch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erreur au démarrage OrderService : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            container.shutdown();
        }
    }

    static class OrderServiceApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(
                OrderController.class,
                Guards.class,
                ApiMapper.class,
                AuthContext.class,
                DatabaseFactory.class,
                OrderRepository.class,
                OrderItemRepository.class,
                PlantRepository.class,
                GatewayAuthFilter.class,
                BaseRepository.class
            );
        }
    }
}
