package security;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import model.User;

@Service
public class SessionService {
    private final Map<String, User> sessions = new ConcurrentHashMap<>();

    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        return sessionId;
    }

    public User getSession(String sessionId) {
        if (sessionId == null) return null;
        return sessions.get(sessionId);
    }

    public void deleteSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    public void removeSession(String sessionId) {
        deleteSession(sessionId);
    }

    public Map<String, User> getSessions() {
        return sessions;
    }
}