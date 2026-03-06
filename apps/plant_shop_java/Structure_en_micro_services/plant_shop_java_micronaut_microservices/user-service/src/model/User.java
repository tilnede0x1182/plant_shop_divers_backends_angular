package model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.sql.Timestamp;

/**
 * Entité représentant un utilisateur.
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
     * @param id Identifiant de l'utilisateur
     * @param name Nom de l'utilisateur
     * @param email Email de l'utilisateur
     * @param passwordHash Hash du mot de passe
     * @param isAdmin true si admin
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
     * Constructeur pour insertion (id et createdAt générés par la DB).
     * @param name Nom de l'utilisateur
     * @param email Email de l'utilisateur
     * @param passwordHash Hash du mot de passe
     * @param isAdmin true si admin
     */
    public User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }

    /**
     * Vérifie si l'utilisateur est admin.
     * @return true si admin
     */
    public boolean isAdmin() {
        return isAdmin;
    }

    /**
     * Définit le statut admin de l'utilisateur.
     * @param admin true pour admin
     */
    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
    }
}
