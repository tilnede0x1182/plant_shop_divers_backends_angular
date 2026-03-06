package model;

import java.sql.Timestamp;

/** Modele representant un utilisateur */
public class User {
    public final int id;
    public final String name;
    public final String email;
    public final String passwordHash;
    public final boolean isAdmin;
    public final Timestamp createdAt;

    /** Constructeur complet */
    public User(int id, String name, String email, String passwordHash, boolean isAdmin, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
    }

    /** Factory pour creation d'un nouvel utilisateur */
    public static User forCreation(String name, String email, String passwordHash, boolean isAdmin) {
        return new User(0, name, email, passwordHash, isAdmin, null);
    }
}
