package repositories;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import java.sql.*;
import models.Plant;

/**
 * Repository pour les plantes.
 * Gere les operations CRUD sur la table plants.
 */
@Dependent
public class PlantRepository extends BaseRepository<Plant> {

    /**
     * Constructeur avec injection de la connexion.
     *
     * @param db Connexion a la base de donnees
     */
    @Inject
    public PlantRepository(Connection db) {
        super(db, "plants");
    }

    /**
     * Mappe un ResultSet vers un objet Plant.
     *
     * @param rs ResultSet positionne sur une ligne
     * @return Objet Plant
     * @throws SQLException En cas d'erreur de lecture
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
     * Cree une nouvelle plante.
     *
     * @param p Plante a creer
     * @return ID de la plante creee
     * @throws SQLException En cas d'erreur d'insertion
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
     * Met a jour une plante.
     *
     * @param p Plante a mettre a jour
     * @throws SQLException En cas d'erreur de mise a jour
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
     * Met a jour le stock d'une plante.
     *
     * @param id ID de la plante
     * @param newStock Nouvelle valeur du stock
     * @throws SQLException En cas d'erreur de mise a jour
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
