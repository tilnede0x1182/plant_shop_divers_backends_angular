package repository;

import io.micronaut.context.annotation.Bean;
import jakarta.inject.Singleton;
import java.sql.*;
import model.User;

/**
 * Repository pour les utilisateurs.
 */
@Singleton
public final class UserRepository extends BaseRepository<User> {

    /**
     * Constructeur.
     * @param db Connection Connexion DB
     */
    public UserRepository(Connection db) {
        super(db, "users");
    }

    /**
     * Mappe un ResultSet vers un User (sans mot de passe).
     * @param rs ResultSet Résultat SQL
     * @return User Utilisateur
     * @throws SQLException En cas d erreur SQL
     */
    @Override
    protected User mapFromResultSet(ResultSet rs) throws SQLException {
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
     * Trouve un utilisateur par email avec mot de passe.
     * @param email String Email
     * @return User Utilisateur ou null
     * @throws SQLException En cas d erreur SQL
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
                        rs.getString("password_hash"),
                        rs.getBoolean("is_admin"),
                        rs.getTimestamp("created_at")
                    );
                }
                return null;
            }
        }
    }

    /**
     * Crée un utilisateur.
     * @param u User Utilisateur
     * @return int ID généré
     * @throws SQLException En cas d erreur SQL
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
     * Met à jour un utilisateur.
     * @param u User Utilisateur
     * @throws SQLException En cas d erreur SQL
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
