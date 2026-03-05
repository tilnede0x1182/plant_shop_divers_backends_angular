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
public class SessionService {

    private final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    /** Retourne la map des sessions actives. */
    public Map<String, Integer> getSessions() {
        return sessions;
    }

    /** Supprime une session. */
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }
}
