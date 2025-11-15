package util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
        map.put("price", toDecimal(plant.price));
        map.put("stock", plant.stock);
        map.put("createdAt", toIso(plant.createdAt));
        return map;
    }

    public static List<Map<String, Object>> toPlantList(List<Plant> plants) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Plant p : plants) {
            list.add(toPlant(p));
        }
        return list;
    }

    private static String toDecimal(BigDecimal bd) {
        return bd != null ? bd.toPlainString() : "0";
    }

    private static String toIso(Timestamp ts) {
        return (ts != null) ?
            ts.toInstant().atOffset(ZoneOffset.UTC).toString() : null;
    }
}
