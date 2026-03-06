package repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.User;

/**
 * Repository pour la gestion des utilisateurs en base.
 */
public final class UserRepository {

    private final Connection db;

    /**
     * Construit le repository avec la connexion BDD.
     * @param db Connexion à la base de données
     */
    public UserRepository(Connection db) {
        this.db = db;
    }

    /**
     * Recherche un utilisateur par son ID.
     * @param id Identifiant de l'utilisateur
     * @return Utilisateur trouvé ou null
     * @throws SQLException En cas d'erreur BDD
     */
    public User find(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFromResultSet(rs);
                }
                return null;
            }
        }
    }

    /**
     * Liste tous les utilisateurs.
     * @return Liste des utilisateurs
     * @throws SQLException En cas d'erreur BDD
     */
    public List<User> list() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapFromResultSet(rs));
            }
        }
        return users;
    }

    /**
     * Supprime un utilisateur par son ID.
     * @param id Identifiant de l'utilisateur
     * @throws SQLException En cas d'erreur BDD
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Convertit un ResultSet en objet User.
     * @param rs ResultSet positionné sur une ligne
     * @return Objet User correspondant
     * @throws SQLException En cas d'erreur de lecture
     */
    private User mapFromResultSet(ResultSet rs) throws SQLException {
        // Ce mapping de base exclut le hash du mot de passe pour des raisons de sécurité
        // lors de la récupération de listes d'utilisateurs ou d'un utilisateur public.
        // Note: Le constructeur de User attend `createdAt`, qui doit être dans le SELECT.
        return new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            null, // passwordHash est volontairement laissé à null
            rs.getBoolean("is_admin"),
            rs.getTimestamp("created_at")
        );
    }

    /**
     * Récupère un utilisateur par son email, en incluant son hash de mot de passe.
     * Cette méthode est spécifiquement destinée au processus d'authentification.
     * @param email L'email de l'utilisateur à trouver.
     * @return L'objet User complet avec son passwordHash, ou null si non trouvé.
     * @throws SQLException
     */
    public User findByEmailWithPassword(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Ce mapping inclut le hash du mot de passe, nécessaire pour la vérification.
                    return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"), // Le hash est inclus ici
                        rs.getBoolean("is_admin"),
                        rs.getTimestamp("created_at")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Insère un nouvel utilisateur en base.
     * @param u Utilisateur à insérer
     * @return ID généré de l'utilisateur
     * @throws SQLException En cas d'erreur d'insertion
     */
    public int create(User u) throws SQLException {
        String sql = "INSERT INTO users(name, email, password_hash, is_admin) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.name);
            ps.setString(2, u.email);
            ps.setString(3, u.passwordHash);
            ps.setBoolean(4, u.isAdmin);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Met à jour un utilisateur existant.
     * @param u Utilisateur à mettre à jour
     * @throws SQLException En cas d'erreur BDD
     */
    public void update(User u) throws SQLException {
        boolean updatePassword = u.passwordHash != null;
        String sql = updatePassword
            ? "UPDATE users SET name=?, email=?, is_admin=?, password_hash=? WHERE id=?"
            : "UPDATE users SET name=?, email=?, is_admin=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, u.name);
            ps.setString(2, u.email);
            ps.setBoolean(3, u.isAdmin);
            if (updatePassword) {
                ps.setString(4, u.passwordHash);
                ps.setInt(5, u.id);
            } else {
                ps.setInt(4, u.id);
            }
            ps.executeUpdate();
        }
    }
}
