package repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de base abstraite pour les repositories.
 * @param <T> Type du modèle
 */
public abstract class BaseRepository<T> {

    protected final Connection db;
    protected final String tableName;

    /**
     * Constructeur.
     * @param db Connection Connexion DB
     * @param tableName String Nom de la table
     */
    public BaseRepository(Connection db, String tableName) {
        this.db = db;
        this.tableName = tableName;
    }

    /**
     * Mappe un ResultSet vers un objet.
     * @param rs ResultSet Résultat SQL
     * @return T Objet mappé
     * @throws SQLException En cas d erreur SQL
     */
    protected abstract T mapFromResultSet(ResultSet rs) throws SQLException;

    /**
     * Trouve un élément par ID.
     * @param id int Identifiant
     * @return T Élément ou null
     * @throws SQLException En cas d erreur SQL
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
     * Liste tous les éléments.
     * @return List Liste d éléments
     * @throws SQLException En cas d erreur SQL
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
     * Supprime un élément par ID.
     * @param id int Identifiant
     * @throws SQLException En cas d erreur SQL
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
