package models;

import java.sql.Timestamp;

/**
 * Modèle représentant un utilisateur.
 */
public final class User {
    public int id;
    public String name;
    public String email;
    public String passwordHash; // null si non chargé
    public boolean isAdmin;
    public Timestamp createdAt; // null lors de l’insertion

    /* constructeur complet (lecture DB) */
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

    /* constructeur pour insertion (id et createdAt générés par la DB) */
    /**
     * Constructeur pour insertion (id et createdAt générés par DB).
     * @param name Nom
     * @param email Email
     * @param passwordHash Hash du mot de passe
     * @param isAdmin Statut admin
     */
    public User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }

    /**
     * Constructeur par défaut.
     */
    public User() {}

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
    }
}
