package models;

import java.sql.Timestamp;

public final class User {
    public int id;
    public String name;
    public String email;
    public String password;
    public String passwordHash;
    public boolean isAdmin;
    public Timestamp createdAt;

    public User(int id, String name, String email, String passwordHash, boolean isAdmin, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
    }

    public User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }

    public User() {}
}
