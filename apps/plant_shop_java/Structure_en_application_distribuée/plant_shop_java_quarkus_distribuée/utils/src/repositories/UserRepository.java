package repositories;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.sql.*;
import models.User;
import util.DatabaseFactory; // Import pour la connexion
import util.ForwardedIdentityHolder; // Ajout si besoin

@Dependent
/**
 * Repository pour les opérations sur les utilisateurs.
 */
public class UserRepository extends BaseRepository<User> {

    @Inject
    /**
     * Constructeur avec injection de connexion.
     * @param db Connexion à la base de données
     */
    public UserRepository(Connection db) {
        super(db, "users");
    }

    @Override
    /**
     * Mappe un ResultSet vers un User.
     * @param rs ResultSet à mapper
     * @return User créé
     * @throws SQLException En cas d'erreur SQL
     */
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
     * Trouve un utilisateur par email avec le hash du mot de passe.
     * @param email Email à rechercher
     * @return User trouvé ou null
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
     * Crée un nouvel utilisateur.
     * @param u Utilisateur à créer
     * @return ID de l'utilisateur créé
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
     * Met à jour un utilisateur.
     * @param u Utilisateur à mettre à jour
     * @throws SQLException En cas d'erreur SQL
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
