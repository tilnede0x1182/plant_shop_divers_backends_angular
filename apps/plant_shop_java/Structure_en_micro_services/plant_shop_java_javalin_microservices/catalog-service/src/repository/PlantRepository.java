// src/repository/PlantRepository.java
package repository;

import java.sql.*;
import model.Plant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository pour la gestion des plantes en base.
 */
public final class PlantRepository {

    private final Connection db;

    /**
     * Constructeur avec connexion à la base de données.
     * @param db Connexion à la base de données
     */
    public PlantRepository(Connection db) {
        this.db = db;
    }

    /**
     * Trouve une plante par son ID.
     * @param id Identifiant de la plante
     * @return Plante trouvée ou null
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
     * Liste toutes les plantes.
     * @return Liste des plantes
     */
    public List<Plant> list() throws SQLException {
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
     * Supprime une plante par son ID.
     * @param id Identifiant de la plante
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
     * @return Objet Plant
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
     * Crée une nouvelle plante en base.
     * @param p Plante à créer
     * @return ID généré
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
     * Met à jour une plante existante.
     * @param p Plante avec les nouvelles données
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
     * Met à jour uniquement le stock d'une plante.
     * @param id Identifiant de la plante
     * @param newStock Nouveau stock
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
