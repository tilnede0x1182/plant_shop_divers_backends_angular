package repository;

import java.sql.*;
import java.util.*;
import model.Plant;

public final class PlantRepository {

    private final Connection db;

    public PlantRepository(Connection db) { this.db = db; }

    public int create(Plant p) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "INSERT INTO plants(name,description,price,stock) VALUES (?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, p.name);
        ps.setString(2, p.description);
        ps.setBigDecimal(3, p.price);
        ps.setInt(4, p.stock);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys(); rs.next();
        return rs.getInt(1);
    }

    public Plant find(int id) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "SELECT * FROM plants WHERE id=?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) return null;
        return new Plant(rs.getInt("id"),
                         rs.getString("name"),
                         rs.getString("description"),
                         rs.getBigDecimal("price"),
                         rs.getInt("stock"));
    }

    public List<Plant> list() throws SQLException {
        Statement st = db.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM plants");
        List<Plant> out = new ArrayList<Plant>();
        while (rs.next())
            out.add(new Plant(rs.getInt("id"),
                              rs.getString("name"),
                              rs.getString("description"),
                              rs.getBigDecimal("price"),
                              rs.getInt("stock")));
        return out;
    }

    public void update(Plant p) throws SQLException {
        PreparedStatement ps = db.prepareStatement(
            "UPDATE plants SET name=?, description=?, price=?, stock=? WHERE id=?");
        ps.setString(1, p.name);
        ps.setString(2, p.description);
        ps.setBigDecimal(3, p.price);
        ps.setInt(4, p.stock);
        ps.setInt(5, p.id);
        ps.executeUpdate();
    }

    public void delete(int id) throws SQLException {
        PreparedStatement ps = db.prepareStatement("DELETE FROM plants WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }
}
