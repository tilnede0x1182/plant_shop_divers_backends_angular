package util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Plant;

/**
 * Utilitaire de mapping pour l'API.
 */
public final class ApiMapper {

    /** Constructeur privé. */
    private ApiMapper() {}

    /**
     * Convertit une plante en Map pour l'API.
     * @param plant Plant Plante à convertir
     * @return Map Représentation JSON
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
     * Convertit une liste de plantes en List de Maps.
     * @param plants List Liste de plantes
     * @return List Liste de Maps
     */
    public static List<Map<String, Object>> toPlantList(List<Plant> plants) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Plant p : plants) {
            list.add(toPlant(p));
        }
        return list;
    }

    /**
     * Convertit un BigDecimal en Double.
     * @param bd BigDecimal Valeur à convertir
     * @return Double Valeur convertie
     */
    private static Double toDecimal(BigDecimal bd) {
        return bd == null ? null : bd.doubleValue();
    }

    /**
     * Convertit un Timestamp en String ISO.
     * @param ts Timestamp Valeur à convertir
     * @return String Format ISO
     */
    private static String toIso(Timestamp ts) {
        return (ts != null) ?
            ts.toInstant().atOffset(ZoneOffset.UTC).toString() : null;
    }
}
