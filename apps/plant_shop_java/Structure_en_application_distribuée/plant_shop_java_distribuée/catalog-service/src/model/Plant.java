import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.Instant;

public final class Plant {
    int id;
    String name;
    String description;
    BigDecimal price;
    int stock;
    Instant createdAt;

    public Plant(int id, String name, String description, BigDecimal price, int stock, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
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
