import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository de base pour le service de catalogue.
 * 
 * @param <T> Type de l entité gérée
 */
abstract class CatalogBaseRepository<T> {
    protected final Connection db;
    protected final String table;

    /**
	 * Constructeur.
	 * 
	 * @param db Connection Connexion à la base de données
	 * @param table String Nom de la table
	 */
    CatalogBaseRepository(Connection db, String table) {
        this.db = db;
        this.table = table;
    }

    /**
	 * Convertit un ResultSet en entité.
	 * 
	 * @param rs ResultSet Le résultat de la requête
	 * @return T L entité mappée
	 * @throws SQLException En cas d erreur SQL
	 */
    abstract T map(ResultSet rs) throws SQLException;

    /**
	 * Trouve une entité par ID.
	 * 
	 * @param id int L ID de l entité
	 * @return T L entité trouvée ou null
	 * @throws SQLException En cas d erreur SQL
	 */
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

    /**
	 * Liste toutes les entités.
	 * 
	 * @return List<T> La liste des entités
	 * @throws SQLException En cas d erreur SQL
	 */
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

    /**
	 * Supprime une entité par ID.
	 * 
	 * @param id int L ID de l entité
	 * @throws SQLException En cas d erreur SQL
	 */
    void delete(int id) throws SQLException {
        try (PreparedStatement ps = db.prepareStatement("DELETE FROM " + table + " WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

/**
 * Repository pour les plantes.
 */
public final class PlantRepository extends CatalogBaseRepository<Plant> {

    /**
	 * Constructeur.
	 * 
	 * @param db Connection Connexion à la base de données
	 */
    public PlantRepository(Connection db) {
        super(db, "plants");
    }

    /**
	 * Mappe un ResultSet vers un objet Plant.
	 * @param rs Le ResultSet
	 * @return La plante mappée
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
	 * 
	 * @param plant Plant La plante à créer
	 * @return int L ID généré
	 * @throws SQLException En cas d erreur SQL
	 */
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

    /**
	 * Met à jour une plante existante.
	 * 
	 * @param plant Plant La plante à mettre à jour
	 * @throws SQLException En cas d erreur SQL
	 */
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
