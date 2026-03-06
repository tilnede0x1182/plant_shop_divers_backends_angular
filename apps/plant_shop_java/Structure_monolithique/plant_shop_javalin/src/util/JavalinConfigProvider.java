package util;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.util.Set;

/**
 * Fournisseur de configuration centralisée pour Javalin.
 * Centralise la configuration CORS, JSON mapper, et autres middlewares.
 */
public final class JavalinConfigProvider {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
        "http://localhost:8300",
        "http://127.0.0.1:8300"
    );

    /** Constructeur privé pour empêcher l'instanciation. */
    private JavalinConfigProvider() {}

    /**
     * Configure les paramètres Javalin (JSON mapper, content-type par défaut).
     *
     * @param config JavalinConfig Configuration Javalin à modifier
     */
    public static void configureJavalin(JavalinConfig config) {
        config.jsonMapper(new JavalinJsonMapper());
        config.http.defaultContentType = "application/json; charset=utf-8";
    }

    /**
     * Middleware CORS pour les requêtes normales.
     */
    public static Handler corsBeforeHandler() {
        return ctx -> {
            String origin = ctx.header("Origin");
            if (isAllowedOrigin(origin)) {
                ctx.header("Access-Control-Allow-Origin", origin);
                ctx.header("Access-Control-Allow-Credentials", "true");
                ctx.header("Vary", "Origin");
            }
        };
    }

    /**
     * Handler pour les requêtes OPTIONS (preflight CORS).
     */
    public static Handler corsPreflightHandler() {
        return ctx -> {
            String origin = ctx.header("Origin");
            if (isAllowedOrigin(origin)) {
                ctx.header("Access-Control-Allow-Origin", origin);
                ctx.header("Access-Control-Allow-Credentials", "true");
                ctx.header("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
                String requestHeaders = ctx.header("Access-Control-Request-Headers");
                if (requestHeaders != null && !requestHeaders.isBlank()) {
                    ctx.header("Access-Control-Allow-Headers", requestHeaders);
                } else {
                    ctx.header("Access-Control-Allow-Headers", "Content-Type");
                }
                ctx.header("Vary", "Origin");
            } else {
                ctx.header("Access-Control-Allow-Headers", "Content-Type");
            }
            ctx.status(204);
        };
    }

    /**
     * Vérifie si l'origine est autorisée.
     *
     * @param origin String L'origine de la requête
     * @return boolean true si l'origine est autorisée
     */
    public static boolean isAllowedOrigin(String origin) {
        return origin != null && ALLOWED_ORIGINS.contains(origin);
    }
}
