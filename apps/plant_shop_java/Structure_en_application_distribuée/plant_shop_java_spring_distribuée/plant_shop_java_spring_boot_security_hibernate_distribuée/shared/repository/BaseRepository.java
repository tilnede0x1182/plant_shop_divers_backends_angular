package repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseRepository<T> {

    protected final Connection db;
    protected final String tableName;

    public BaseRepository(Connection db, String tableName) {
        this.db = db;
        this.tableName = tableName;
    }

    protected abstract T mapFromResultSet(ResultSet rs) throws SQLException;

    public T find(int id) throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE id=?";
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

    public List<T> list() throws SQLException {
        String sql = "SELECT * FROM " + tableName;
        List<T> results = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(mapFromResultSet(rs));
            }
        }
        return results;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
