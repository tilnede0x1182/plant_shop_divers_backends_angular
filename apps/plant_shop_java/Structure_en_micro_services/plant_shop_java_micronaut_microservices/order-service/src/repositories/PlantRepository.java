package repository;

import jakarta.inject.Singleton;
import java.sql.*;
import model.PlantStock;

@Singleton
public final class PlantRepository {

    private final Connection db;

    public PlantRepository(Connection db) {
        this.db = db;
    }

    public PlantStock find(int id) throws SQLException {
        String sql = "SELECT id, name, price, stock FROM plants WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PlantStock(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getBigDecimal("price"),
                        rs.getInt("stock")
                    );
                }
                return null;
            }
        }
    }
}
