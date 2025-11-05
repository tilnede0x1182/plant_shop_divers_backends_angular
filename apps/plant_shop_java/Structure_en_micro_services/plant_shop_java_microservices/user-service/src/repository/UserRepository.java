package user.repository;

import user.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class UserRepository {
    private final Connection db;

    public UserRepository(Connection db) {
        this.db = db;
    }

    public User find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM users WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    public List<User> list() throws SQLException {
        List<User> out = new ArrayList<>();
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users")) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    public int create(User user) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO users(name, email, password_hash, is_admin) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.name());
            ps.setString(2, user.email());
            ps.setString(3, user.passwordHash());
            ps.setBoolean(4, user.isAdmin());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void update(User user) throws SQLException {
        boolean updatePassword = user.passwordHash() != null && !user.passwordHash().isBlank();
        String sql = updatePassword
            ? "UPDATE users SET name=?, email=?, is_admin=?, password_hash=? WHERE id=?"
            : "UPDATE users SET name=?, email=?, is_admin=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, user.name());
            ps.setString(2, user.email());
            ps.setBoolean(3, user.isAdmin());
            if (updatePassword) {
                ps.setString(4, user.passwordHash());
                ps.setInt(5, user.id());
            } else {
                ps.setInt(4, user.id());
            }
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getBoolean("is_admin"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
        );
    }
}
