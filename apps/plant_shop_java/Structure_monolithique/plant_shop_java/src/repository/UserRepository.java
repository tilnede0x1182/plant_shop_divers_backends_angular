package repository;

import java.sql.*;
import model.User;

/**
 * Repository pour les utilisateurs.
 */
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
     * @return User Utilisateur créé
     * @throws SQLException En cas d erreur SQL
     */
    @Override
    protected User mapFromResultSet(ResultSet rs) throws SQLException {
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
     * Crée un utilisateur.
     * @param u User Utilisateur à créer
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
     * Met à jour un utilisateur (sans mot de passe).
     * @param u User Utilisateur à mettre à jour
     * @throws SQLException En cas d erreur SQL
     */
    public void update(User u) throws SQLException {
        // Cette mise à jour ne modifie pas le mot de passe.
        String sql = "UPDATE users SET name=?, email=?, is_admin=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, u.name);
            ps.setString(2, u.email);
            ps.setBoolean(3, u.isAdmin);
            ps.setInt(4, u.id);
            ps.executeUpdate();
        }
    }
}
