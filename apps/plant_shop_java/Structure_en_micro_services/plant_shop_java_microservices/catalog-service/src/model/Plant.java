package catalog.model;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.Instant;

public record Plant(
    int id,
    String name,
    String description,
    BigDecimal price,
    int stock,
    Instant createdAt
) {
    public Plant withId(int value) {
        return new Plant(value, name, description, price, stock, createdAt);
    }

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

    public Plant withName(String value) {
        return new Plant(id, value, description, price, stock, createdAt);
    }

    public Plant withDescription(String value) {
        return new Plant(id, name, value, price, stock, createdAt);
    }

    public Plant withPrice(BigDecimal value) {
        return new Plant(id, name, description, value, stock, createdAt);
    }

    public Plant withStock(int value) {
        return new Plant(id, name, description, price, value, createdAt);
    }

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
