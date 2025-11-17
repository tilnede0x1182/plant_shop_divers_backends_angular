package util;

import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Plant;

public final class ApiMapper {

    private ApiMapper() {}

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

    public static List<Map<String, Object>> toPlantList(List<Plant> plants) {
        return plants.stream().map(ApiMapper::toPlant).toList();
    }

    private static String toIso(Timestamp ts) {
        return ts == null ? null : ts.toInstant().atOffset(ZoneOffset.UTC).toString();
    }
}
