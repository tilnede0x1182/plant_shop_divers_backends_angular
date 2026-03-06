package security;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import model.User;

/**
 * Service de gestion des sessions en memoire.
 */
@Service
public class SessionService {
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    /**
     * Cree une nouvelle session pour un utilisateur.
     *
     * @param user Utilisateur
     * @return ID de session genere
     */
    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        return sessionId;
    }

    /**
     * Recupere un utilisateur depuis son ID de session.
     *
     * @param sessionId ID de session
     * @return Utilisateur ou null
     */
    public User getSession(String sessionId) {
        if (sessionId == null) return null;
        return sessions.get(sessionId);
    }

    /**
     * Supprime une session.
     *
     * @param sessionId ID de session
     */
    public void deleteSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    /**
     * Alias pour deleteSession.
     *
     * @param sessionId ID de session
     */
    public void removeSession(String sessionId) {
        deleteSession(sessionId);
    }

    /**
     * Retourne la map des sessions actives.
     *
     * @return Map des sessions
     */
    public Map<String, User> getSessions() {
        return sessions;
    }
}