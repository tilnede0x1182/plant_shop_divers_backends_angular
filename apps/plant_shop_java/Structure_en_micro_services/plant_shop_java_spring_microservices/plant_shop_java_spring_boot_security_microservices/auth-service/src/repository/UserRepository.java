package repository;

import model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;
import java.sql.*;

/**
 * Repository pour la gestion des utilisateurs en base de données.
 * Fournit les opérations CRUD sur la table users.
 */
@Repository
@RequestScope
public class UserRepository {

    private final Connection db;

    /**
     * Constructeur avec injection de la connexion BDD.
     * @param db Connexion à la base de données
     */
    @Autowired
    public UserRepository(Connection db) {
        this.db = db;
    }

    /**
     * Recherche un utilisateur par son identifiant.
     * @param id Identifiant de l'utilisateur
     * @return L'utilisateur trouvé ou null
     * @throws SQLException En cas d'erreur SQL
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
     * Mappe un ResultSet vers un objet User.
     * @param rs ResultSet positionné sur une ligne
     * @return L'utilisateur mappé
     * @throws SQLException En cas d'erreur SQL
     */
    private User mapFromResultSet(ResultSet rs) throws SQLException {
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
     * Recherche un utilisateur par email avec son hash de mot de passe.
     * @param email Adresse email de l'utilisateur
     * @return L'utilisateur avec passwordHash ou null
     * @throws SQLException En cas d'erreur SQL
     */
    public User findByEmailWithPassword(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
     * Crée un nouvel utilisateur en base de données.
     * @param u Utilisateur à créer
     * @return L'identifiant généré
     * @throws SQLException En cas d'erreur SQL
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
     * @param u Utilisateur avec les nouvelles valeurs
     * @throws SQLException En cas d'erreur SQL
     */
    public void update(User u) throws SQLException {
        boolean updatePassword = u.passwordHash != null && !u.passwordHash.isEmpty();
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
