package model;

import java.sql.Timestamp;

/**
 * Modèle représentant un utilisateur pour le service commandes.
 * Version simplifiée utilisée pour les vérifications d'autorisation.
 */
public class User {
    public final int id;
    public final String name;
    public final String email;
    public final String passwordHash;
    public final boolean isAdmin;
    public final Timestamp createdAt;

    /**
     * Constructeur complet pour un utilisateur.
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
     * Factory method pour créer un utilisateur sans id ni date.
     * @param name Nom de l'utilisateur
     * @param email Adresse email
     * @param passwordHash Hash du mot de passe
     * @param isAdmin Statut administrateur
     * @return Nouvel utilisateur
     */
    public static User forCreation(String name, String email, String passwordHash, boolean isAdmin) {
        return new User(0, name, email, passwordHash, isAdmin, null);
    }
}
