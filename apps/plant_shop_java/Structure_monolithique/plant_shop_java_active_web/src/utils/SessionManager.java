package util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private static final Map<String, Long> SESSIONS = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static String createSession(Number userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId ne peut pas être null");
        }
        String id = UUID.randomUUID().toString();
        SESSIONS.put(id, userId.longValue());
        return id;
    }

    public static Long getUserId(String sessionId) {
        if (sessionId == null) return null;
        return SESSIONS.get(sessionId);
    }

    public static void removeSession(String sessionId) {
        if (sessionId == null) return;
        SESSIONS.remove(sessionId);
    }
}
