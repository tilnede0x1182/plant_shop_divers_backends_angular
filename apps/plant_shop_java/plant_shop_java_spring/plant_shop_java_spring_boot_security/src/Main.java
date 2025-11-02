import jakarta.servlet.Filter;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.servlet.DispatcherServlet;
import security.CorsFilter;
import security.SessionAuthFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static final Path ENV_FILE = Path.of("config", ".env");

    public static void main(String[] args) throws Exception {
        Map<String, String> env = loadEnv();
        int port = parsePort(env.getOrDefault("SERVER_ADDRESS",
            env.getOrDefault("SERVER_ADRRESS", "4100")));

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(determineBaseDir());
        tomcat.setPort(port);
        tomcat.getConnector();

        StandardContext context = (StandardContext) tomcat.addContext("", new File(".").getAbsolutePath());
        context.addLifecycleListener(new Tomcat.FixContextListener());
        context.addApplicationListener("org.springframework.web.context.request.RequestContextListener");

        AnnotationConfigWebApplicationContext applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.scan("controllers", "repositories", "security", "utils");
        applicationContext.setServletContext(context.getServletContext());
        applicationContext.refresh();

        DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext);
        Wrapper dispatcher = Tomcat.addServlet(context, "dispatcher", dispatcherServlet);
        dispatcher.setLoadOnStartup(1);
        context.addServletMappingDecoded("/", "dispatcher");

        registerFilter(context, "requestContextFilter", new RequestContextFilter());
        registerFilter(context, "corsFilter", applicationContext.getBean(CorsFilter.class));
        registerFilter(context, "sessionAuthFilter", applicationContext.getBean(SessionAuthFilter.class));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                tomcat.stop();
            } catch (Exception ignored) {
            }
            applicationContext.close();
        }));

        tomcat.start();
        System.out.println("🚀 Serveur Spring MVC démarré sur http://localhost:" + port);
        tomcat.getServer().await();
    }

    private static void registerFilter(StandardContext context, String name, Filter filter) {
        FilterDef definition = new FilterDef();
        definition.setFilterName(name);
        definition.setFilter(filter);
        definition.setFilterClass(filter.getClass().getName());
        context.addFilterDef(definition);

        FilterMap mapping = new FilterMap();
        mapping.setFilterName(name);
        mapping.addURLPattern("/*");
        context.addFilterMap(mapping);
    }

    private static Map<String, String> loadEnv() {
        if (!Files.exists(ENV_FILE)) {
            return Map.of();
        }
        Map<String, String> values = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(ENV_FILE)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (!key.isEmpty()) {
                        values.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️  Impossible de lire config/.env : " + e.getMessage());
        }
        return values;
    }

    private static int parsePort(String rawPort) {
        if (rawPort == null || rawPort.isBlank()) {
            return 4100;
        }
        String candidate = rawPort.contains(":") ? rawPort.substring(rawPort.indexOf(':') + 1) : rawPort;
        try {
            return Integer.parseInt(candidate.trim());
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Valeur de port invalide (" + rawPort + "), utilisation de 4100.");
            return 4100;
        }
    }

    private static String determineBaseDir() throws IOException {
        Path tempDir = Files.createTempDirectory("tomcat");
        tempDir.toFile().deleteOnExit();
        return tempDir.toAbsolutePath().toString();
    }
}
