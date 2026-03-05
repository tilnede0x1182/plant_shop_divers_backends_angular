package security;

import org.springframework.http.HttpStatus;

/**
 * Exception pour les erreurs d'authentification.
 */
public class AuthException extends RuntimeException {
    private final HttpStatus status;

    /**
     * Constructeur avec statut HTTP et message.
     *
     * @param status HttpStatus Statut HTTP de l'erreur
     * @param message String Message d'erreur
     */
    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** Retourne le statut HTTP associé. */
    public HttpStatus getStatus() {
        return status;
    }
}
