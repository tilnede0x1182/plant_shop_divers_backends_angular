package util;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Plant;

/**
 * Utilitaire de conversion des entités vers des structures JSON.
 * Transforme les objets métier en Map pour la sérialisation.
 */
public final class ApiMapper {

    /**
     * Constructeur privé pour empêcher l'instanciation.
     */
    private ApiMapper() {}

    /**
     * Convertit une plante en Map pour la réponse JSON.
     * @param plant Plante à convertir
     * @return Map représentant la plante
     */
    public static Map<String, Object> toPlant(Plant plant) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", plant.id);
        map.put("name", plant.name);
        map.put("description", plant.description);
        map.put("price", DecimalMapper.toDecimal(plant.price));
        map.put("stock", plant.stock);
        map.put("createdAt", toIso(plant.createdAt));
        return map;
    }

    /**
     * Convertit une liste de plantes en liste de Maps.
     * @param plants Liste des plantes à convertir
     * @return Liste de Maps
     */
    public static List<Map<String, Object>> toPlantList(List<Plant> plants) {
        return plants.stream().map(ApiMapper::toPlant).toList();
    }

    /**
     * Convertit un Timestamp en chaîne ISO 8601.
     * @param ts Timestamp à convertir
     * @return Chaîne ISO ou null
     */
    private static String toIso(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
}
