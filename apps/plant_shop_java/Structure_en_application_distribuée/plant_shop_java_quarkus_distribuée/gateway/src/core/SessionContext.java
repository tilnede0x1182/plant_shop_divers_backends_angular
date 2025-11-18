package core;

/**
 * Contient les informations de sécurité résolues par la Gateway
 * avant de forwarder la requête aux microservices.
 */
public record SessionContext(Integer userId, boolean isAdmin) {

    public static SessionContext anonymous() {
        return new SessionContext(null, false);
    }

    public boolean isAuthenticated() {
        return userId != null;
    }
}
