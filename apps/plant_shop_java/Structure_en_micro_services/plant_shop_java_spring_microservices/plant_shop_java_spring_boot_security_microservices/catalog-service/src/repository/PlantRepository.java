package repository;

import model.Plant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour la gestion des plantes en base de données.
 * Fournit les opérations CRUD sur la table plants.
 */
@Repository
@RequestScope
public class PlantRepository {

    private final Connection db;

    /**
     * Constructeur avec injection de la connexion BDD.
     * @param db Connexion à la base de données
     */
    @Autowired
    public PlantRepository(Connection db) {
        this.db = db;
    }

    /**
     * Recherche une plante par son identifiant.
     * @param id Identifiant de la plante
     * @return La plante trouvée ou null
     * @throws SQLException En cas d'erreur SQL
     */
    public Plant find(int id) throws SQLException {
        String sql = "SELECT * FROM plants WHERE id=?";
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
     * Récupère toutes les plantes du catalogue.
     * @return Liste des plantes triées par id
     * @throws SQLException En cas d'erreur SQL
     */
    public List<Plant> findAll() throws SQLException {
        String sql = "SELECT * FROM plants ORDER BY id";
        List<Plant> plants = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                plants.add(mapFromResultSet(rs));
            }
        }
        return plants;
    }

    /**
     * Supprime une plante par son identifiant.
     * @param id Identifiant de la plante à supprimer
     * @throws SQLException En cas d'erreur SQL
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM plants WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Mappe un ResultSet vers un objet Plant.
     * @param rs ResultSet positionné sur une ligne
     * @return La plante mappée
     * @throws SQLException En cas d'erreur SQL
     */
    private Plant mapFromResultSet(ResultSet rs) throws SQLException {
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
     * Crée une nouvelle plante en base de données.
     * @param p Plante à créer
     * @return L'identifiant généré
     * @throws SQLException En cas d'erreur SQL
     */
    public int create(Plant p) throws SQLException {
        String sql = "INSERT INTO plants(name, description, price, stock) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.name);
            ps.setString(2, p.description != null ? p.description : ""); // Gère null
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
     * Met à jour une plante existante.
     * @param p Plante avec les nouvelles valeurs
     * @throws SQLException En cas d'erreur SQL
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
     * @param id Identifiant de la plante
     * @param newStock Nouvelle quantité en stock
     * @throws SQLException En cas d'erreur SQL
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
