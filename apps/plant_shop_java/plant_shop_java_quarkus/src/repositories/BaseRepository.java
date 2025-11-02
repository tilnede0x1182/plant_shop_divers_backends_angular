package repositories;

import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseRepository<T> {

    // Injecté par la sous-classe
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
        List<T> results = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
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
