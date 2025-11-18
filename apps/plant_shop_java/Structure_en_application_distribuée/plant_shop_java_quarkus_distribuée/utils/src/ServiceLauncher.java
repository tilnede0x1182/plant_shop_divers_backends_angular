package util;

import java.util.Map;

public final class ServiceLauncher {

    private ServiceLauncher() {}

    public static void run(String serviceName, String envPortKey, int defaultPort, String[] args) {
        Map<String, String> env = System.getenv();
        String port = env.getOrDefault(envPortKey, String.valueOf(defaultPort));
        System.setProperty("quarkus.http.port", port);
        System.out.printf("[%s] prêt sur http://localhost:%s (placeholder)\n", serviceName, port);
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException ignored) {
        }
    }
}
