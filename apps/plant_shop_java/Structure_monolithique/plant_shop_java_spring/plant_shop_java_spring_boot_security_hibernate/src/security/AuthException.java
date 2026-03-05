package security;

import org.springframework.http.HttpStatus;

/**
 * Exception pour les erreurs d'authentification.
 */
public class AuthException extends RuntimeException {
    private final HttpStatus status;

    /** Constructeur avec statut HTTP et message. */
    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /** Retourne le statut HTTP associé. */
    public HttpStatus getStatus() {
        return status;
    }
}
