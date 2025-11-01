import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.javalite.activeweb.RequestDispatcher;
import javax.servlet.DispatcherType;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import utils.CorsUtil;

public class Application {

    public static void main(String[] args) throws Exception {
        Map<String, String> env = loadEnv();
        int port = parsePort(env.getOrDefault("SERVER_ADDRESS", "4100"));

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/api");
        server.setHandler(context);

        // Active le CORS avant le filtre ActiveWeb
        CorsUtil.enable(context);

        // Configuration du filtre ActiveWeb
        FilterHolder filterHolder = new FilterHolder(new RequestDispatcher());
        filterHolder.setInitParameter("exclusions", "css,images,js");
        context.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

				int maxRetries = 10;
				int attempt = 0;
				while (true) {
						try {
								server.start();
								break;
						} catch (java.net.BindException be) {
								System.err.println("Port " + port + " occupé : " + be.getMessage());
								attempt++;
								if (attempt > maxRetries) {
										System.err.println("Échec après " + maxRetries + " tentatives. Abandon.");
										throw be;
								}
								port++;
								System.err.println("Réessai sur le port " + port + " (tentative " + attempt + ")");
								server.stop();
								server = new Server(port);
								ServletContextHandler newContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
								newContext.setContextPath(context.getContextPath());
								server.setHandler(newContext);
								utils.CorsUtil.enable(newContext);
								FilterHolder newFilterHolder = new FilterHolder(new org.javalite.activeweb.RequestDispatcher());
								newFilterHolder.setInitParameter("exclusions", "css,images,js");
								newContext.addFilter(newFilterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));
								context = newContext;
						}
				}
				System.out.println("🚀 Serveur JavaLite ActiveWeb démarré sur http://localhost:" + port);
				server.join();
    }

    private static Map<String, String> loadEnv() {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("config/.env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                int i = line.indexOf('=');
                if (i > 0) {
                    map.put(line.substring(0, i).trim(), line.substring(i + 1).trim());
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️  Impossible de lire le fichier config/.env : " + e.getMessage());
            System.err.println("→ Les variables d'environnement par défaut seront utilisées (port 4100).");
        }
        return map;
    }

    private static int parsePort(String value) {
        try {
            if (value.contains(":")) {
                return Integer.parseInt(value.split(":")[1]);
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 4100; // Port par défaut
        }
    }
}
