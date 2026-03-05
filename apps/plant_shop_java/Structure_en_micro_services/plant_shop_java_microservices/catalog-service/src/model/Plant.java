package catalog.model;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Record représentant une plante.
 * @param id ID de la plante
 * @param name Nom
 * @param description Description
 * @param price Prix
 * @param stock Stock
 * @param createdAt Date de création
 */
public record Plant(
    int id,
    String name,
    String description,
    BigDecimal price,
    int stock,
    Instant createdAt
) {
    /**
     * Crée une copie avec un nouvel ID.
     * @param value Nouvel ID
     * @return Nouvelle instance
     */
    public Plant withId(int value) {
        return new Plant(value, name, description, price, stock, createdAt);
    }

    /**
     * Crée une plante depuis un objet JSON.
     * @param json Objet JSON source
     * @return Plante créée
     */
    public static Plant fromJson(JSONObject json) {
        // Constructeur simple pour la création (ID sera 0)
        return new Plant(
            json.optInt("id", 0),
            json.getString("name"),
            json.optString("description", null),
            json.getBigDecimal("price"),
            json.optInt("stock", 0),
            null
        );
    }

    /**
     * Crée une copie avec un nouveau nom.
     * @param value Nouveau nom
     * @return Nouvelle instance
     */
    public Plant withName(String value) {
        return new Plant(id, value, description, price, stock, createdAt);
    }

    /**
     * Crée une copie avec une nouvelle description.
     * @param value Nouvelle description
     * @return Nouvelle instance
     */
    public Plant withDescription(String value) {
        return new Plant(id, name, value, price, stock, createdAt);
    }

    /**
     * Crée une copie avec un nouveau prix.
     * @param value Nouveau prix
     * @return Nouvelle instance
     */
    public Plant withPrice(BigDecimal value) {
        return new Plant(id, name, description, value, stock, createdAt);
    }

    /**
     * Crée une copie avec un nouveau stock.
     * @param value Nouveau stock
     * @return Nouvelle instance
     */
    public Plant withStock(int value) {
        return new Plant(id, name, description, price, value, createdAt);
    }

    /**
     * Convertit la plante en JSON.
     * @return Objet JSON
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("description", description == null ? JSONObject.NULL : description);
        json.put("price", price);
        json.put("stock", stock);
        if (createdAt != null) {
            json.put("createdAt", createdAt.toString());
        }
        return json;
    }
}
