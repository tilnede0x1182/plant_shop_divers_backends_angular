package utils;

import controllers.AuthController;
import controllers.OrderController;
import controllers.PlantController;
import controllers.UserController;
import io.undertow.Undertow;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Application;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import security.CorsConfig;
import security.SessionAuthFilter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

@Singleton
public final class QuarkusBootstrap {

    @Inject
    AuthController authController;

    @Inject
    PlantController plantController;

    @Inject
    UserController userController;

    @Inject
    OrderController orderController;

    @Inject
    SessionAuthFilter sessionAuthFilter;

    @Inject
    CorsConfig corsConfig;

    public void run(String[] args) throws Exception {
        Map<String, String> env = loadEnv();
        int port = parsePort(env.getOrDefault("SERVER_ADDRESS", "4100"));

        if (!isPortAvailable(port)) {
            System.err.println("❌ Le port " + port + " est déjà utilisé. Impossible de démarrer le serveur.");
            System.exit(1);
        }

        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");

        System.out.println("🚀 Serveur RESTEasy/Undertow sur http://localhost:" + port);

        UndertowJaxrsServer server = new UndertowJaxrsServer();
        server.start(Undertow.builder().addHttpListener(port, "0.0.0.0"));

        ResteasyDeployment deployment = new ResteasyDeploymentImpl();
        deployment.setApplication(new Application() {});
        deployment.getRegistry().addSingletonResource(authController);
        deployment.getRegistry().addSingletonResource(plantController);
        deployment.getRegistry().addSingletonResource(userController);
        deployment.getRegistry().addSingletonResource(orderController);
        deployment.getProviderFactory().register(sessionAuthFilter);
        deployment.getProviderFactory().register(corsConfig);

        server.deploy(deployment);

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    }

    private Map<String, String> loadEnv() throws IOException {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("config/.env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                int i = line.indexOf('=');
                if (i > 0) map.put(line.substring(0, i).trim(), line.substring(i + 1).trim());
            }
        } catch (IOException e) {
            System.err.println("⚠️  Fichier config/.env introuvable, utilisation des valeurs par défaut.");
        }
        return map;
    }

    private int parsePort(String value) {
        try {
            if (value.contains(":")) return Integer.parseInt(value.split(":")[1]);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Valeur SERVER_ADDRESS invalide, utilisation du port 4100.");
            return 4100;
        }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
