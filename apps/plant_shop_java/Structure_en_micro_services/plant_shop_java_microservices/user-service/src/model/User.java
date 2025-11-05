package user.model;

import org.json.JSONObject;
import java.time.Instant;

public record User(
    int id,
    String name,
    String email,
    String passwordHash,
    boolean isAdmin,
    Instant createdAt
) {
    public User withName(String value) {
        return new User(id, value, email, passwordHash, isAdmin, createdAt);
    }

    public User withEmail(String value) {
        return new User(id, name, value, passwordHash, isAdmin, createdAt);
    }

    public User withPasswordHash(String value) {
        return new User(id, name, email, value, isAdmin, createdAt);
    }

    public User withAdmin(boolean value) {
        return new User(id, name, email, passwordHash, value, createdAt);
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("email", email);
        json.put("admin", isAdmin);
        if (createdAt != null) {
            json.put("createdAt", createdAt.toString());
        }
        return json;
    }
}
