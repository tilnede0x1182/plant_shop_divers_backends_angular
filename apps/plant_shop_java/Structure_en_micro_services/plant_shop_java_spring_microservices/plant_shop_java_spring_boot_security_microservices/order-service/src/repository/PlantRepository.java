package repository;

import model.PlantStock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.*;

/**
 * Repository pour la lecture des plantes depuis le service commandes.
 * Fournit uniquement la consultation du stock pour les validations.
 */
@Repository
public final class PlantRepository {

    @Autowired
    private Connection db;

    /**
     * Recherche une plante par son identifiant.
     * @param id Identifiant de la plante
     * @return La plante avec son stock ou null
     * @throws SQLException En cas d'erreur SQL
     */
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
