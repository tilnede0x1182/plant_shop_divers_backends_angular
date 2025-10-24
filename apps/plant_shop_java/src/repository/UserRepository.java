package repository;

import java.sql.*;
import java.util.*;
import model.User;

public final class UserRepository {

    private final Connection db;

    public UserRepository(Connection db) { this.db = db; }

    public int create(User u) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "INSERT INTO users(name,email,password_hash,is_admin) VALUES (?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, u.name);
        ps.setString(2, u.email);
        ps.setString(3, u.passwordHash);
        ps.setBoolean(4, u.isAdmin);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys(); rs.next();
        return rs.getInt(1);
    }

    public User find(int id) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "SELECT id,name,email,is_admin FROM users WHERE id=?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return null;
        return new User(rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        null,
                        rs.getBoolean("is_admin"));
    }

    public List<User> list() throws SQLException {
        Statement st = db.createStatement();
        ResultSet rs = st.executeQuery("SELECT id,name,email,is_admin FROM users");
        List<User> out = new ArrayList<User>();
        while (rs.next())
            out.add(new User(rs.getInt("id"),
                             rs.getString("name"),
                             rs.getString("email"),
                             null,
                             rs.getBoolean("is_admin")));
        return out;
    }

    public void update(User u) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "UPDATE users SET name=?, email=?, is_admin=? WHERE id=?");
        ps.setString(1, u.name);
        ps.setString(2, u.email);
        ps.setBoolean(3, u.isAdmin);
        ps.setInt(4, u.id);
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        PreparedStatement ps = db.prepareStatement("DELETE FROM users WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
