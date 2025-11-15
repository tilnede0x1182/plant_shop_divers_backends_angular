import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Point d'entrée principal de l'application Quarkus.
 */
@QuarkusMain
public class Application implements QuarkusApplication {

    @Inject
    @ConfigProperty(name = "quarkus.http.port", defaultValue = "4100")
    int port;

    public static void main(String[] args) {
        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        if (!isPortAvailable(port)) {
            System.err.println("❌ Le port " + port + " est déjà utilisé. Impossible de démarrer le serveur.");
            return 1;
        }

        System.out.println("🚀 Serveur Quarkus sur http://localhost:" + port);
        Quarkus.waitForExit();
        return 0;
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
