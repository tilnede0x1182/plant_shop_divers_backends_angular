package security;

import jakarta.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean singleton pour stocker les sessions actives
 * (mapping entre l'ID de session (cookie) et l'ID utilisateur).
 */
@Singleton
public class SessionService {

    private final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    /**
     * Retourne la map des sessions actives.
     * @return Map session ID vers user ID
     */
    public Map<String, Integer> getSessions() {
        return sessions;
    }
}
