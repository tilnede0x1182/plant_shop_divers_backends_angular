package model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.sql.Timestamp;

/**
 * Modèle représentant un utilisateur.
 */
@Introspected
@Serdeable
public final class User {
    public int id;
    public String name;
    public String email;
    public String passwordHash; // null si non chargé
    public boolean isAdmin;
    public Timestamp createdAt; // null lors de l’insertion

    /**
     * Constructeur complet (lecture DB).
     * @param id int Identifiant
     * @param name String Nom
     * @param email String Email
     * @param passwordHash String Hash du mot de passe
     * @param isAdmin boolean Si admin
     * @param createdAt Timestamp Date de création
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
     * @param name String Nom
     * @param email String Email
     * @param passwordHash String Hash du mot de passe
     * @param isAdmin boolean Si admin
     */
    public User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }

    /**
 * Retourne le statut admin.
 * @return true si admin
 */
public boolean isAdmin() {
        return isAdmin;
    }

    /**
     * Définit le statut admin.
     * @param admin boolean Nouveau statut
     */
    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
    }
}
