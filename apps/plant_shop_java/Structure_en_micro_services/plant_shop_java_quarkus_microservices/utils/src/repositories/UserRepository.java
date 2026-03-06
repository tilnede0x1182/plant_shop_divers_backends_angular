package repositories;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.sql.*;
import models.User;
import util.DatabaseFactory; // Import pour la connexion
import util.ForwardedIdentityHolder; // Ajout si besoin

/**
 * Repository pour les utilisateurs.
 * Gere les operations CRUD sur la table users.
 */
@Dependent
public class UserRepository extends BaseRepository<User> {

    /**
     * Constructeur avec injection de la connexion.
     *
     * @param db Connexion a la base de donnees
     */
    @Inject
    public UserRepository(Connection db) {
        super(db, "users");
    }

    /**
     * Mappe un ResultSet vers un objet User (sans password).
     *
     * @param rs ResultSet positionne sur une ligne
     * @return Objet User
     * @throws SQLException En cas d'erreur de lecture
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
     * Recherche un utilisateur par email avec son password hash.
     *
     * @param email Email de l'utilisateur
     * @return User avec passwordHash ou null
     * @throws SQLException En cas d'erreur de lecture
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
     * Cree un nouvel utilisateur.
     *
     * @param u Utilisateur a creer
     * @return ID de l'utilisateur cree
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
     * Met a jour un utilisateur.
     *
     * @param u Utilisateur a mettre a jour
     * @throws SQLException En cas d'erreur de mise a jour
     */
    public void update(User u) throws SQLException {
        boolean updatePassword = u.passwordHash != null && !u.passwordHash.isEmpty();
        String sql = updatePassword
            ?
            "UPDATE users SET name=?, email=?, is_admin=?, password_hash=? WHERE id=?"
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
