package util;

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

    /** Constructeur privé pour empêcher l'instanciation. */
    private CorsUtil() {}

    /**
     * Active CORS pour le contexte servlet.
     * @param context ServletContextHandler Contexte à configurer
     */
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
