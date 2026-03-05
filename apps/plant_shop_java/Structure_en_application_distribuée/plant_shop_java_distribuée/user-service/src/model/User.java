import org.json.JSONObject;

import java.time.Instant;

/**
 * Modèle représentant un utilisateur.
 */
public final class User {
    int id;
    String name;
    String email;
    String passwordHash;
    boolean isAdmin;
    Instant createdAt;

    /**
	 * Constructeur complet.
	 * @param id Identifiant
	 * @param name Nom
	 * @param email Email
	 * @param passwordHash Hash du mot de passe
	 * @param isAdmin Si administrateur
	 * @param createdAt Date de création
	 */
	public User(int id, String name, String email, String passwordHash, boolean isAdmin, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
    }

    /**
	 * Convertit en JSON.
	 * @return L'objet JSON
	 */
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
