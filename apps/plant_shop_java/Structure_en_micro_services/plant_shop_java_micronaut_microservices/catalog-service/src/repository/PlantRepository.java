package repository;

import io.micronaut.context.annotation.Bean;
import jakarta.inject.Singleton;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import model.Plant;
import java.math.BigDecimal;

/**
 * Repository pour la gestion des plantes en base.
 */
@Singleton
public final class PlantRepository {

    private final Connection db;

    /**
     * Construit le repository avec la connexion BDD.
     * @param db Connexion à la base de données
     */
    public PlantRepository(Connection db) {
        this.db = db;
    }

    /**
     * Recherche une plante par son ID.
     * @param id Identifiant de la plante
     * @return Plante trouvée ou null
     * @throws SQLException En cas d'erreur BDD
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
     * @throws SQLException En cas d'erreur BDD
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
     * @throws SQLException En cas d'erreur BDD
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM plants WHERE id=?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Convertit un ResultSet en objet Plant.
     * @param rs ResultSet positionné sur une ligne
     * @return Objet Plant correspondant
     * @throws SQLException En cas d'erreur de lecture
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
     * Insère une nouvelle plante en base.
     * @param p Plante à insérer
     * @return ID généré de la plante
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
     * Met à jour une plante existante.
     * @param p Plante à mettre à jour
     * @throws SQLException En cas d'erreur BDD
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
     * @param newStock Nouveau stock
     * @throws SQLException En cas d'erreur BDD
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
