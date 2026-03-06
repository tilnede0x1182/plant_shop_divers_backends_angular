package repository;

import java.sql.*;
import model.PlantStock;

/**
 * Repository pour accéder aux données des plantes (lecture seule).
 */
public final class PlantRepository {

    private final Connection db;

    /**
     * Construit le repository avec la connexion BDD.
     * @param db Connexion à la base de données
     */
    public PlantRepository(Connection db) {
        this.db = db;
    }

    /**
     * Recherche une plante par son ID.
     * @param id Identifiant de la plante
     * @return PlantStock trouvé ou null
     * @throws SQLException En cas d'erreur BDD
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
