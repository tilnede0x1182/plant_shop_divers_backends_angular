// src/repository/PlantRepository.java
package repository;

import java.sql.*;
import model.Plant;
import java.math.BigDecimal;
import catalog.repositories.BaseRepository;

/**
 * Repository pour les plantes.
 */
public final class PlantRepository extends BaseRepository<Plant> {

    /**
	 * Constructeur.
	 * @param db Connexion à la base de données
	 */
	public PlantRepository(Connection db) {
        super(db, "plants");
    }

    /**
	 * Mappe un ResultSet vers une Plant.
	 * @param rs Le ResultSet
	 * @return La plante mappée
	 */
	@Override
    protected Plant mapFromResultSet(ResultSet rs) throws SQLException {
        return new Plant(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getBigDecimal("price"),
            rs.getInt("stock"),
            rs.getTimestamp("created_at")
        );
    }

    /**
	 * Crée une plante.
	 * @param p La plante à créer
	 * @return L'identifiant généré
	 */
	public int create(Plant p) throws SQLException {
        String sql = "INSERT INTO plants(name, description, price, stock) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.name);
            ps.setString(2, p.description);
            ps.setBigDecimal(3, p.price);
            ps.setInt(4, p.stock);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
	 * Met à jour une plante.
	 * @param p La plante à mettre à jour
	 */
	public void update(Plant p) throws SQLException {
        String sql = "UPDATE plants SET name=?, description=?, price=?, stock=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, p.name);
            ps.setString(2, p.description);
            ps.setBigDecimal(3, p.price);
            ps.setInt(4, p.stock);
            ps.setInt(5, p.id);
            ps.executeUpdate();
        }
    }

    /**
	 * Met à jour le stock d'une plante.
	 * @param id L'identifiant
	 * @param newStock Le nouveau stock
	 */
	public void updateStock(int id, int newStock) throws SQLException {
        String sql = "UPDATE plants SET stock=? WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, newStock);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}
