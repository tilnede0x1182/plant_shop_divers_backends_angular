import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Modèle représentant une plante.
 */
public final class Plant {
    int id;
    String name;
    String description;
    BigDecimal price;
    int stock;
    Instant createdAt;

    /**
	 * Constructeur.
	 * 
	 * @param id int L ID de la plante
	 * @param name String Le nom de la plante
	 * @param description String La description
	 * @param price BigDecimal Le prix
	 * @param stock int Le stock disponible
	 * @param createdAt Instant La date de création
	 */
    public Plant(int id, String name, String description, BigDecimal price, int stock, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.createdAt = createdAt;
    }

    /**
	 * Convertit la plante en objet JSON.
	 * 
	 * @return JSONObject La représentation JSON
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
