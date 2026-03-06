package model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

/**
 * Entité JPA représentant un utilisateur.
 */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;

    @Column(nullable = false)
    public String name;

    @Column(nullable = false, unique = true)
    public String email;

    // Ignoré lors de la sérialisation (jamais renvoyé)
    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    public String passwordHash;

    @Column(name = "is_admin", nullable = false)
    public boolean isAdmin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public Timestamp createdAt;

    // Champ "password" en écriture seule pour register/create
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Transient
    public String password;

    /**
     * Constructeur complet.
     * @param id int Identifiant
     * @param name String Nom
     * @param email String Email
     * @param passwordHash String Hash mot de passe
     * @param isAdmin boolean Est admin
     * @param createdAt Timestamp Date création
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
     * Constructeur pour création.
     * @param name String Nom
     * @param email String Email
     * @param passwordHash String Hash mot de passe
     * @param isAdmin boolean Est admin
     */
    public User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }
    /** Constructeur par défaut pour JPA. */
    public User() {}
}
