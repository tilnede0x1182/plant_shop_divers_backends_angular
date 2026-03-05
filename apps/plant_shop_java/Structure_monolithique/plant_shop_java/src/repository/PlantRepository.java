package repository;

import java.sql.*;
import model.Plant;

/**
 * Repository pour les plantes.
 */
public final class PlantRepository extends BaseRepository<Plant> {

    /**
     * Constructeur.
     * @param db Connection Connexion DB
     */
    public PlantRepository(Connection db) {
        super(db, "plants");
    }

    /**
     * Mappe un ResultSet vers un Plant.
     * @param rs ResultSet Résultat SQL
     * @return Plant Plante créée
     * @throws SQLException En cas d erreur SQL
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
     * @param p Plant Plante à créer
     * @return int ID généré
     * @throws SQLException En cas d erreur SQL
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
     * @param p Plant Plante à mettre à jour
     * @throws SQLException En cas d erreur SQL
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
}
