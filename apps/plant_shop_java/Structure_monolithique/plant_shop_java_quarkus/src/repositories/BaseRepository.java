package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository de base fournissant les opérations CRUD communes.
 * @param <T> Type de l'entité gérée
 */
public abstract class BaseRepository<T> {

    protected final Connection db;
    protected final String tableName;

    /**
     * Constructeur avec connexion et nom de table.
     * @param db Connexion à la base de données
     * @param tableName Nom de la table
     */
    public BaseRepository(Connection db, String tableName) {
        this.db = db;
        this.tableName = tableName;
    }

    /**
     * Mappe une ligne du ResultSet vers une entité.
     * @param rs ResultSet positionné sur la ligne à mapper
     * @return L'entité mappée
     */
    protected abstract T mapFromResultSet(ResultSet rs) throws SQLException;

    /**
     * Trouve une entité par son ID.
     * @param id ID de l'entité
     * @return L'entité ou null si non trouvée
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
     * Liste toutes les entités.
     * @return Liste des entités
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
     * Supprime une entité par son ID.
     * @param id ID de l'entité à supprimer
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
