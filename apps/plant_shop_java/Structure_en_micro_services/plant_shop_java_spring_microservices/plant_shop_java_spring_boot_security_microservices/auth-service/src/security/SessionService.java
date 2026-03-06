package security;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de gestion des sessions utilisateurs.
 * Stocke les associations sessionId -> userId en mémoire.
 */
@Service
public class SessionService {
    private final Map<String, Integer> sessions = new ConcurrentHashMap<>();

    /**
     * Retourne la map des sessions actives.
     * @return Map des sessions (sessionId -> userId)
     */
    public Map<String, Integer> getSessions() {
        return sessions;
    }

    /**
     * Récupère l'identifiant utilisateur associé à une session.
     * @param sessionId Identifiant de session
     * @return L'identifiant utilisateur ou null
     */
    public Integer getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return sessions.get(sessionId);
    }

    /**
     * Supprime une session active.
     * @param sessionId Identifiant de la session à supprimer
     */
    public void removeSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }
}
