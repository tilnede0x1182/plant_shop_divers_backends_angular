package catalog.repository;

import catalog.model.Plant;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository de base avec opérations CRUD communes.
 * @param <T> Type d'entité
 */
abstract class CatalogBaseRepository<T> {
    protected final Connection db;
    protected final String table;

    CatalogBaseRepository(Connection db, String table) {
        this.db = db;
        this.table = table;
    }

    abstract T map(ResultSet rs) throws SQLException;

    // Rendu public pour accès direct par le Controller
    /**
     * Trouve une entité par ID.
     * @param id ID à rechercher
     * @return Entité ou null
     * @throws SQLException En cas d'erreur SQL
     */
    public T find(int id) throws SQLException {
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

    /**
	 * Trouve toutes les entités.
	 * @return Liste de toutes les entités
	 * @throws SQLException En cas d'erreur SQL
	 */
	public List<T> findAll() throws SQLException {
        return findAllOrderedBy(null);
    }

    /**
     * Trouve toutes les entités avec tri optionnel.
     * @param orderClause Clause ORDER BY
     * @return Liste des entités
     * @throws SQLException En cas d'erreur SQL
     */
    protected List<T> findAllOrderedBy(String orderClause) throws SQLException {
        List<T> out = new ArrayList<>();
        String sql = "SELECT * FROM " + table;
        if (orderClause != null && !orderClause.isBlank()) {
            sql += " ORDER BY " + orderClause;
        }

        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(map(rs));
            }
        }
        return out;
    }

    /**
     * Supprime une entité par ID.
     * @param id ID à supprimer
     * @throws SQLException En cas d'erreur SQL
     */
    void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

/**
 * Repository pour les opérations sur les plantes.
 */
public final class PlantRepository extends CatalogBaseRepository<Plant> {

    /**
     * Constructeur.
     * @param db Connexion à la base de données
     */
    public PlantRepository(Connection db) {
        super(db, "plants");
    }

    /**
     * Mappe un ResultSet vers une Plant.
     * @param rs ResultSet à mapper
     * @return Plante créée
     * @throws SQLException En cas d'erreur SQL
     */
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

    /**
     * Crée une nouvelle plante.
     * @param plant Plante à créer
     * @return ID de la plante créée
     * @throws SQLException En cas d'erreur SQL
     */
    public int create(Plant plant) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "INSERT INTO plants(name, description, price, stock) VALUES (?, ?, ?, ?)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, plant.name());
            ps.setString(2, plant.description());
            ps.setBigDecimal(3, plant.price());
            ps.setInt(4, plant.stock());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /**
     * Met à jour une plante.
     * @param plant Plante à mettre à jour
     * @throws SQLException En cas d'erreur SQL
     */
    public void update(Plant plant) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(
            "UPDATE plants SET name=?, description=?, price=?, stock=? WHERE id=?")) {
            ps.setString(1, plant.name());
            ps.setString(2, plant.description());
            ps.setBigDecimal(3, plant.price());
            ps.setInt(4, plant.stock());
            ps.setInt(5, plant.id());
            ps.executeUpdate();
        }
    }

    /**
     * Trouve toutes les plantes triées par nom.
     * @return Liste des plantes
     * @throws SQLException En cas d'erreur SQL
     */
    public List<Plant> findAllOrderedByName() throws SQLException {
        return findAllOrderedBy("name ASC");
    }

    /**
     * Supprime une plante par ID.
     * @param id ID de la plante
     * @throws SQLException En cas d'erreur SQL
     */
    public void delete(int id) throws SQLException {
        super.delete(id);
    }
}
