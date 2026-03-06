package util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import model.Plant;

/**
 * ApiMapper local pour catalog-service avec uniquement les méthodes nécessaires
 */
public final class ApiMapper {

    /**
     * Constructeur privé pour classe utilitaire.
     */
    private ApiMapper() {}

    /**
     * Convertit une plante en Map JSON.
     * @param plant Plante à convertir
     * @return Map représentant le JSON
     */
    public static Map<String, Object> toPlant(Plant plant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", plant.id);
        map.put("name", plant.name);
        map.put("description", plant.description);
        map.put("price", toDecimal(plant.price));
        map.put("stock", plant.stock);
        map.put("createdAt", toIso(plant.createdAt));
        return map;
    }

    /**
     * Convertit un BigDecimal en Double.
     * @param value Valeur à convertir
     * @return Double ou null
     */
    private static Double toDecimal(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    /**
     * Convertit un Timestamp en chaîne ISO.
     * @param timestamp Timestamp à convertir
     * @return Chaîne ISO ou null
     */
    private static String toIso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
}
