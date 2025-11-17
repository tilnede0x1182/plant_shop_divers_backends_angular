package utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;
import java.util.Optional;

@ApplicationScoped
public final class ServerLifecycleLogger {

    void onStart(@Observes StartupEvent event) {
        int port = resolvePort();
        System.out.printf("🚀 Serveur Quarkus disponible sur http://localhost:%d%n", port);
    }

    void onStop(@Observes ShutdownEvent event) {
        System.out.println("🛑 Arrêt du serveur Quarkus.");
    }

    private int resolvePort() {
        return Optional.ofNullable(System.getProperty("quarkus.http.port"))
            .flatMap(value -> {
                try {
                    return Optional.of(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    return Optional.empty();
                }
            })
            .orElse(4100);
    }
}
