package utils;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;

import javax.servlet.DispatcherType;
import java.util.EnumSet;

/**
 * Configuration CORS centralisée pour ActiveWeb.
 * Ne dépend d’aucun fichier web.xml, ni d’aucun filtre externe.
 */
public final class CorsUtil {

    private CorsUtil() {
        // Utilitaire, pas d’instanciation
    }

    public static void enable(ServletContextHandler context) {
        FilterHolder cors = context.addFilter(CrossOriginFilter.class, "/*",
                EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));

        cors.setInitParameter("allowedOrigins", "*");
        cors.setInitParameter("allowedMethods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
        cors.setInitParameter("allowedHeaders", "Content-Type,Authorization,X-Requested-With");
        cors.setInitParameter("allowCredentials", "true");
        cors.setInitParameter("chainPreflight", "false");
    }
}
