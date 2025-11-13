package model;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.sql.Timestamp;

@Introspected
@Serdeable
public final class User {
    public int id;
    public String name;
    public String email;
    public String passwordHash; // null si non chargé
    public boolean isAdmin;
    public Timestamp createdAt; // null lors de l’insertion

    /* constructeur complet (lecture DB) */
    public User(int id, String name, String email, String passwordHash, boolean isAdmin, Timestamp createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.isAdmin = isAdmin;
        this.createdAt = createdAt;
    }

    /* constructeur pour insertion (id et createdAt générés par la DB) */
    public User(String name, String email, String passwordHash, boolean isAdmin) {
        this(0, name, email, passwordHash, isAdmin, null);
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        this.isAdmin = admin;
    }
}
