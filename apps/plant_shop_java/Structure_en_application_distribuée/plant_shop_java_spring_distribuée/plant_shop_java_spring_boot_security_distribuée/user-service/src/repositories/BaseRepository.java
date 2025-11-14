package user.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de base abstraite pour les repositories afin de réduire la duplication de code (DRY).
 * Fournit des implémentations génériques pour les opérations CRUD communes.
 *
 * @param <T> Le type de l'objet modèle géré par le repository (ex: User, Plant).
 */
public abstract class BaseRepository<T> {

    protected final Connection db;
    protected final String tableName;

    /**
     * Constructeur pour le BaseRepository.
     * @param db La connexion à la base de données.
     * @param tableName Le nom de la table associée à ce repository.
     */
    public BaseRepository(Connection db, String tableName) {
        this.db = db;
        this.tableName = tableName;
    }

    /**
     * Méthode abstraite que les classes filles doivent implémenter.
     * Elle définit comment mapper une ligne du ResultSet vers un objet de type T.
     * @param rs Le ResultSet positionné sur la ligne à mapper.
     * @return Un objet de type T instancié avec les données du ResultSet.
     * @throws SQLException Si une erreur d'accès SQL se produit.
     */
    protected abstract T mapFromResultSet(ResultSet rs) throws SQLException;

    /**
     * Récupère une entité par son identifiant.
     * @param id L'identifiant de l'entité à trouver.
     * @return L'objet T trouvé, ou null s'il n'existe pas.
     * @throws SQLException Si une erreur d'accès SQL se produit.
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
     * Récupère la liste de toutes les entités de la table.
     * @return Une liste d'objets de type T.
     * @throws SQLException Si une erreur d'accès SQL se produit.
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
     * Supprime une entité par son identifiant.
     * @param id L'identifiant de l'entité à supprimer.
     * @throws SQLException Si une erreur d'accès SQL se produit.
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
