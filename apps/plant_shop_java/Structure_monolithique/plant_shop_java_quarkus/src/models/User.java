package models;

import java.sql.Timestamp;

/**
 * Modèle représentant un utilisateur.
 */
public final class User {
    public int id;
    public String name;
    public String email;
    public String password; // plain password from payload only
    public String passwordHash; // null si non chargé
    public boolean isAdmin;
    public Timestamp createdAt; // null lors de l’insertion

    /**
     * Constructeur complet (lecture DB).
     *
     * @param id int Identifiant de l'utilisateur
     * @param name String Nom de l'utilisateur
     * @param email String Email de l'utilisateur
     * @param passwordHash String Hash du mot de passe
     * @param isAdmin boolean Est administrateur
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
     * Constructeur pour insertion (id et createdAt générés par la DB).
     *
     * @param name String Nom de l'utilisateur
     * @param email String Email de l'utilisateur
     * @param passwordHash String Hash du mot de passe
     * @param isAdmin boolean Est administrateur
     */
    public User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }

    // Constructeur par défaut nécessaire pour la désérialisation JSON (ex: POST /users)
    public User() {}
}
