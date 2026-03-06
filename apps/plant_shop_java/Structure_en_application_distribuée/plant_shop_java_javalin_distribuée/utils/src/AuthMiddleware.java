package util;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.UnauthorizedResponse;

/**
 * Wrappe les handlers Javalin avec les vérifications d'authentification
 * basées sur les en-têtes ajoutés par la gateway.
 */
public final class AuthMiddleware {

    /**
 * Constructeur privé - classe utilitaire.
 */
private AuthMiddleware() {
    }

    /**
     * Interface de résolution d'utilisateur.
     */
    @FunctionalInterface
    public interface UserResolver {
        /**
         * Résout l'utilisateur depuis le contexte d'auth.
         * @param auth AuthContext Contexte d'authentification
         * @return Object Utilisateur résolu
         */
        Object resolve(AuthContext auth) throws Exception;
    }

    /**
	 * Exige un utilisateur authentifié.
	 * @param next Handler suivant
	 * @return Handler protégé
	 */
	public static Handler requireUser(Handler next) {
        return requireUser(null, next);
    }

    /**
	 * Exige un utilisateur authentifié avec résolution.
	 * @param resolver Résolveur d'utilisateur
	 * @param next Handler suivant
	 * @return Handler protégé
	 */
	public static Handler requireUser(UserResolver resolver, Handler next) {
        return ctx -> handle(ctx, resolver, next, false);
    }

    /**
	 * Exige un administrateur.
	 * @param next Handler suivant
	 * @return Handler protégé
	 */
	public static Handler requireAdmin(Handler next) {
        return requireAdmin(null, next);
    }

    /**
	 * Exige un administrateur avec résolution.
	 * @param resolver Résolveur d'utilisateur
	 * @param next Handler suivant
	 * @return Handler protégé
	 */
	public static Handler requireAdmin(UserResolver resolver, Handler next) {
        return ctx -> handle(ctx, resolver, next, true);
    }

    /**
	 * Gère l'authentification et appelle le handler.
	 * @param ctx Contexte Javalin
	 * @param resolver Résolveur optionnel
	 * @param next Handler suivant
	 * @param adminOnly true si admin requis
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
