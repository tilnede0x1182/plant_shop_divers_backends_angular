package security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.Map;

/**
 * Gestionnaire global des exceptions.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Gère les exceptions d'authentification.
     * @param ex AuthException Exception à gérer
     * @return ResponseEntity Réponse HTTP avec message d'erreur
     */
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, String>> handleAuthException(AuthException ex) {
        return ResponseEntity
            .status(ex.getStatus())
            .body(Map.of("error", ex.getMessage()));
    }
}
