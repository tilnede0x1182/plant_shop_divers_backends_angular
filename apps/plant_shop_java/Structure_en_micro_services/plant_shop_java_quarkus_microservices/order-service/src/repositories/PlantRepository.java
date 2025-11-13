package repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.*;
import models.PlantStock;

@ApplicationScoped
public final class PlantRepository {

    @Inject
    java.sql.Connection db;

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
