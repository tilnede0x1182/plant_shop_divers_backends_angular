package model;
import java.sql.Timestamp;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Modèle représentant un utilisateur du système.
 * Contient les informations d'identification et le rôle administrateur.
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
     * Constructeur complet pour un utilisateur existant.
     * @param id Identifiant unique
     * @param name Nom de l'utilisateur
     * @param email Adresse email
     * @param passwordHash Hash du mot de passe
     * @param isAdmin Statut administrateur
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
     * Constructeur pour la création d'un nouvel utilisateur.
     * @param name Nom de l'utilisateur
     * @param email Adresse email
     * @param passwordHash Hash du mot de passe
     * @param isAdmin Statut administrateur
     */
    public User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }
    /**
     * Constructeur par défaut pour la désérialisation JSON.
     */
    public User() {}
}
