package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository de base avec operations CRUD generiques.
 *
 * @param <T> Type de l'entite geree
 */
public abstract class BaseRepository<T> {

    protected final Connection db;
    protected final String tableName;

    /**
     * Constructeur.
     *
     * @param db Connexion a la base de donnees
     * @param tableName Nom de la table
     */
    public BaseRepository(Connection db, String tableName) {
        this.db = db;
        this.tableName = tableName;
    }

    protected abstract T mapFromResultSet(ResultSet rs) throws SQLException;

    /**
     * Recherche une entite par son ID.
     *
     * @param id ID de l'entite
     * @return Entite ou null si non trouvee
     * @throws SQLException En cas d'erreur de lecture
     */
    public T find(int id) throws SQLException {
        String sql = "SELECT * FROM " + tableName + " WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapFromResultSet(rs);
                }
                return null;
            }
        }
    }

    /**
     * Liste toutes les entites.
     *
     * @return Liste des entites
     * @throws SQLException En cas d'erreur de lecture
     */
    public List<T> list() throws SQLException {
        List<T> results = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;
        try (Statement st = db.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                results.add(mapFromResultSet(rs));
            }
        }
        return results;
    }

    /**
     * Supprime une entite par son ID.
     *
     * @param id ID de l'entite
     * @throws SQLException En cas d'erreur de suppression
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
