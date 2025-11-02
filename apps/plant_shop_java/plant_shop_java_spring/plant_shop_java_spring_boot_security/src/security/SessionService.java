package security;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.ApplicationScope;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean singleton (@ApplicationScope) pour stocker les sessions actives
 * (mapping entre l'ID de session (cookie) et l'ID utilisateur).
 */
@Service
@ApplicationScope // Garantit un seul instance pour toute l'application
public class SessionService {

    private final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    public Map<String, Integer> getSessions() {
        return sessions;
    }
}
