package user.model;

import org.json.JSONObject;
import java.time.Instant;

/**
 * Représente un utilisateur.
 * @param id ID de l'utilisateur
 * @param name Nom
 * @param email Email
 * @param passwordHash Hash du mot de passe
 * @param isAdmin Est administrateur
 * @param createdAt Date de création
 */
public record User(
    int id,
    String name,
    String email,
    String passwordHash,
    boolean isAdmin,
    Instant createdAt
) {
    /**
	 * Crée une copie avec un nouveau nom.
	 * @param value Nouveau nom
	 * @return Nouvelle instance
	 */
	public User withName(String value) {
        return new User(id, value, email, passwordHash, isAdmin, createdAt);
    }

    /**
	 * Crée une copie avec un nouvel email.
	 * @param value Nouvel email
	 * @return Nouvelle instance
	 */
	public User withEmail(String value) {
        return new User(id, name, value, passwordHash, isAdmin, createdAt);
    }

    /**
	 * Crée une copie avec un nouveau hash.
	 * @param value Nouveau hash
	 * @return Nouvelle instance
	 */
	public User withPasswordHash(String value) {
        return new User(id, name, email, value, isAdmin, createdAt);
    }

    /**
	 * Crée une copie avec un nouveau statut admin.
	 * @param value Nouveau statut
	 * @return Nouvelle instance
	 */
	public User withAdmin(boolean value) {
        return new User(id, name, email, passwordHash, value, createdAt);
    }

    /**
	 * Convertit en JSON.
	 * @return Objet JSON
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
