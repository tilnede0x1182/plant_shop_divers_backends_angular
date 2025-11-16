package util;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;

/**
 * Middleware pour appliquer les règles d'authentification basées sur les entêtes propagés.
 */
public final class AuthMiddleware {

    private AuthMiddleware() {
    }

    @FunctionalInterface
    public interface UserResolver {
        Object resolve(AuthContext auth) throws Exception;
    }

    public static Handler requireUser(Handler next) {
        return requireUser(null, next);
    }

    public static Handler requireUser(UserResolver resolver, Handler next) {
        return ctx -> handle(ctx, resolver, next, false);
    }

    public static Handler requireAdmin(Handler next) {
        return requireAdmin(null, next);
    }

    public static Handler requireAdmin(UserResolver resolver, Handler next) {
        return ctx -> handle(ctx, resolver, next, true);
    }

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
