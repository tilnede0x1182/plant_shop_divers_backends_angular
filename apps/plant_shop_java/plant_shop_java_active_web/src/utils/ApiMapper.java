package util;

import org.javalite.activejdbc.Model;
import org.javalite.common.JsonHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    @SuppressWarnings("unchecked")
    public static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        return (Map<String, Object>) JsonHelper.toMap(json);
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> jsonToListOfMaps(Object value) {
        if (value instanceof List<?>) {
            return (List<Map<String, Object>>) value;
        }
        return Collections.emptyList();
    }
}
