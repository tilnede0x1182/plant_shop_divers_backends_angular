package util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire de sessions utilisateur.
 */
public final class SessionManager {
    private static final Map<String, Long> SESSIONS = new ConcurrentHashMap<>();

    /** Constructeur privé pour empêcher l'instanciation. */
    private SessionManager() {}

    /**
     * Crée une nouvelle session.
     * @param userId Number ID de l'utilisateur
     * @return String ID de session
     */
    public static String createSession(Number userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId ne peut pas être null");
        }
        String id = UUID.randomUUID().toString();
        SESSIONS.put(id, userId.longValue());
        return id;
    }

    /**
     * Récupère l'ID utilisateur d'une session.
     * @param sessionId String ID de session
     * @return Long ID utilisateur ou null
     */
    public static Long getUserId(String sessionId) {
        if (sessionId == null) return null;
        return SESSIONS.get(sessionId);
    }

    /**
     * Supprime une session.
     * @param sessionId String ID de session
     */
    public static void removeSession(String sessionId) {
        if (sessionId == null) return;
        SESSIONS.remove(sessionId);
    }
}
