import controllers.PlantController;
import java.util.concurrent.CountDownLatch;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import repository.PlantRepository;
import catalog.repositories.BaseRepository;
import catalog.util.ApiMapper;
import catalog.security.AuthContext;
import catalog.security.GatewayAuthFilter;
import catalog.security.Guards;
import util.DatabaseFactory;
import jakarta.ws.rs.core.Application;
import java.util.Set;
import io.undertow.Undertow;
import util.EnvLoader;
import util.CdiRequestScopeFilter;

/**
 * Point d'entrée du microservice CatalogService.
 * Configure Weld CDI et démarre un serveur RESTEasy/Undertow sur le port 6102.
 */
public class CatalogService {

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        try {
            int port = Integer.parseInt(EnvLoader.get("CATALOG_SERVICE_PORT", "6102"));

            UndertowJaxrsServer server = new UndertowJaxrsServer();
            Undertow.Builder builder = Undertow.builder().addHttpListener(port, "0.0.0.0");
            server.start(builder);

            ResteasyDeployment deployment = new ResteasyDeploymentImpl();
            deployment.setApplication(new CatalogServiceApplication());
            deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
            server.deploy(deployment);

            System.out.printf("🌱 CatalogService disponible sur http://localhost:%d%n", port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                shutdownLatch.countDown();
                server.stop();
            }));

            shutdownLatch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erreur au démarrage CatalogService : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            container.shutdown();
        }
    }

    static class CatalogServiceApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(
                PlantController.class,
                Guards.class,
                ApiMapper.class,
                AuthContext.class,
                DatabaseFactory.class,
                PlantRepository.class,
                BaseRepository.class,
                GatewayAuthFilter.class,
                CdiRequestScopeFilter.class
            );
        }
    }
}
