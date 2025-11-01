package util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private static final Map<String, Integer> SESSIONS = new ConcurrentHashMap<>();

    private SessionManager() {}

    public static String createSession(Integer userId) {
        String id = UUID.randomUUID().toString();
        SESSIONS.put(id, userId);
        return id;
    }

    public static Integer getUserId(String sessionId) {
        if (sessionId == null) return null;
        return SESSIONS.get(sessionId);
    }

    public static void removeSession(String sessionId) {
        if (sessionId == null) return;
        SESSIONS.remove(sessionId);
    }
}
