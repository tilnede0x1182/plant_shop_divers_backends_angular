package model;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Modèle représentant un utilisateur.
 */
public final class User {
    public int id;
    public String name;
    public String email;

    // Ignoré lors de la sérialisation (jamais renvoyé)
    @JsonIgnore
    public String passwordHash;

    public boolean isAdmin;
    public Timestamp createdAt;

    // Champ "password" en écriture seule pour register/create
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String password;

    /**
     * Constructeur complet (lecture DB).
     * @param id ID de l'utilisateur
     * @param name Nom
     * @param email Email
     * @param passwordHash Hash du mot de passe
     * @param isAdmin Statut admin
     * @param createdAt Date de création
     */
    public User(int id, String name, String email, String passwordHash, boolean isAdmin, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
    }
    /**
     * Constructeur pour insertion.
     * @param name Nom
     * @param email Email
     * @param passwordHash Hash du mot de passe
     * @param isAdmin Statut admin
     */
    public User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }
    public User() {} // Nécessaire pour la désérialisation JSON
}
