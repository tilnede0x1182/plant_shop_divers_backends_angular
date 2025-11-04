import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

abstract class CatalogBaseRepository<T> {
    protected final Connection db;
    protected final String table;

    CatalogBaseRepository(Connection db, String table) {
        this.db = db;
        this.table = table;
    }

    abstract T map(ResultSet rs) throws SQLException;

    T find(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("SELECT * FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return map(rs);
                }
            }
        }
        return null;
    }

    List<T> list() throws SQLException {
        List<T> out = new ArrayList<>();
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM " + table)) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

public final class PlantRepository extends CatalogBaseRepository<Plant> {

    public PlantRepository(Connection db) {
        super(db, "plants");
    }

    @Override
    Plant map(ResultSet rs) throws SQLException {
        return new Plant(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getBigDecimal("price"),
            rs.getInt("stock"),
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
        );
    }

    public int create(Plant plant) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO plants(name, description, price, stock) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plant.name);
            ps.setString(2, plant.description);
            ps.setBigDecimal(3, plant.price);
            ps.setInt(4, plant.stock);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void update(Plant plant) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "UPDATE plants SET name=?, description=?, price=?, stock=? WHERE id=?")) {
            ps.setString(1, plant.name);
            ps.setString(2, plant.description);
            ps.setBigDecimal(3, plant.price);
            ps.setInt(4, plant.stock);
            ps.setInt(5, plant.id);
            ps.executeUpdate();
        }
    }
}
