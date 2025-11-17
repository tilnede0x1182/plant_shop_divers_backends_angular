import controllers.AuthController;
import java.util.concurrent.CountDownLatch;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import auth.security.CorsConfig;
import auth.security.SessionAuthFilter;
import jakarta.ws.rs.core.Application;
import java.util.Set;
import io.undertow.Undertow;
import util.EnvLoader;
import util.CdiRequestScopeFilter;

/**
 * Point d'entrée du microservice AuthService.
 * Configure Weld CDI et démarre un serveur RESTEasy/Undertow sur le port 6101.
 */
public class AuthService {

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        Weld weld = new Weld();
        WeldContainer container = weld.initialize();
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        try {
            int port = Integer.parseInt(EnvLoader.get("AUTH_SERVICE_PORT", "6101"));

            UndertowJaxrsServer server = new UndertowJaxrsServer();
            Undertow.Builder builder = Undertow.builder().addHttpListener(port, "0.0.0.0");
            server.start(builder);

            ResteasyDeployment deployment = new ResteasyDeploymentImpl();
            deployment.setApplication(new AuthServiceApplication());
            deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
            server.deploy(deployment);

            System.out.printf("🔐 AuthService démarré sur http://localhost:%d/api/auth%n", port);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                shutdownLatch.countDown();
                server.stop();
            }));

            shutdownLatch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Erreur au démarrage AuthService : " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            container.shutdown();
        }
    }

    static class AuthServiceApplication extends Application {
        @Override
        public Set<Class<?>> getClasses() {
            return Set.of(
                AuthController.class,
                SessionAuthFilter.class,
                CdiRequestScopeFilter.class,
                CorsConfig.class
            );
        }
    }
}
