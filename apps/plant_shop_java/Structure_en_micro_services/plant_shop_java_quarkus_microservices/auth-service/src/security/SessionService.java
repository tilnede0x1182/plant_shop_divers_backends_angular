package security;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean singleton (@ApplicationScoped) pour stocker les sessions actives
 * (mapping entre l'ID de session (cookie) et l'ID utilisateur).
 */
@ApplicationScoped
public class SessionService {

    private final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    /**
     * Récupère la map des sessions actives.
     *
     * @return Map associant les IDs de session aux IDs utilisateur
     */
    public Map<String, Integer> getSessions() {
        return sessions;
    }
}
