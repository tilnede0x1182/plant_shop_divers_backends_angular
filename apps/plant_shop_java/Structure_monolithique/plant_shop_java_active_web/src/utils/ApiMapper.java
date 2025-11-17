package util;

import org.javalite.activejdbc.Model;
import org.javalite.common.JsonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Petit utilitaire pour conversion map <-> JSON ou Model <-> JSON.
 * Minimal et non intrusif.
 */
public final class ApiMapper {
    private ApiMapper() {}

    public static String toJson(Map<String, Object> m) {
        return JsonHelper.toJsonString(m);
    }

    public static String modelToJson(Model m, String... attrs) {
        if (m == null) return "{}";
        return m.toJson(false, attrs);
    }

    public static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        Map<?, ?> raw = JsonHelper.toMap(json);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        return copyToStringObjectMap(raw);
    }

    public static List<Map<String, Object>> jsonToListOfMaps(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>(list.size());
        for (Object element : list) {
            if (!(element instanceof Map<?, ?> raw)) {
                return Collections.emptyList();
            }
            result.add(copyToStringObjectMap(raw));
        }
        return result;
    }

    private static Map<String, Object> copyToStringObjectMap(Map<?, ?> raw) {
        Map<String, Object> copy = new LinkedHashMap<>(raw.size());
        for (Entry<?, ?> entry : raw.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }
}
