package util;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;

/**
 * Middleware pour appliquer les règles d'authentification basées sur les entêtes propagés.
 */
public final class AuthMiddleware {

    /**
     * Constructeur privé pour classe utilitaire.
     */
    private AuthMiddleware() {
    }

    /**
     * Interface fonctionnelle pour résoudre un utilisateur.
     */
    @FunctionalInterface
    public interface UserResolver {
        Object resolve(AuthContext auth) throws Exception;
    }

    /**
     * Crée un handler exigeant un utilisateur authentifié.
     * @param next Handler suivant
     * @return Handler avec vérification
     */
    public static Handler requireUser(Handler next) {
        return requireUser(null, next);
    }

    /**
     * Crée un handler exigeant un utilisateur avec résolveur.
     * @param resolver Résolveur d'utilisateur
     * @param next Handler suivant
     * @return Handler avec vérification
     */
    public static Handler requireUser(UserResolver resolver, Handler next) {
        return ctx -> handle(ctx, resolver, next, false);
    }

    /**
     * Crée un handler exigeant un admin.
     * @param next Handler suivant
     * @return Handler avec vérification
     */
    public static Handler requireAdmin(Handler next) {
        return requireAdmin(null, next);
    }

    /**
     * Crée un handler exigeant un admin avec résolveur.
     * @param resolver Résolveur d'utilisateur
     * @param next Handler suivant
     * @return Handler avec vérification
     */
    public static Handler requireAdmin(UserResolver resolver, Handler next) {
        return ctx -> handle(ctx, resolver, next, true);
    }

    /**
     * Gère la vérification d'authentification.
     * @param ctx Contexte Javalin
     * @param resolver Résolveur d'utilisateur
     * @param next Handler suivant
     * @param adminOnly true si admin requis
     * @throws Exception En cas d'erreur
     */
    private static void handle(Context ctx, UserResolver resolver, Handler next, boolean adminOnly) throws Exception {
        AuthContext auth = AuthContext.fromHeaders(ctx);
        if (!auth.isAuthenticated()) {
            throw new UnauthorizedResponse("Authentification requise");
        }
        if (adminOnly && !auth.isAdmin()) {
            throw new ForbiddenResponse("Accès refusé");
        }
        ctx.attribute("auth", auth);
        if (resolver != null) {
            Object user = resolver.resolve(auth);
            if (user == null) {
                if (adminOnly) {
                    throw new ForbiddenResponse("Utilisateur introuvable");
                } else {
                    throw new UnauthorizedResponse("Utilisateur introuvable");
                }
            }
            ctx.attribute("user", user);
        }
        next.handle(ctx);
    }
}
