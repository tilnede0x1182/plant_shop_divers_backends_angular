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
    /**
     * Constructeur privé (utilitaire statique).
     */
    private ApiMapper() {}

    /**
     * Convertit une Map en JSON.
     * @param m Map Données à convertir
     * @return String JSON
     */
    public static String toJson(Map<String, Object> m) {
        return JsonHelper.toJsonString(m);
    }

    /**
     * Convertit un Model en JSON.
     * @param m Model Modèle à convertir
     * @param attrs String[] Attributs à inclure
     * @return String JSON
     */
    public static String modelToJson(Model m, String... attrs) {
        if (m == null) return "{}";
        return m.toJson(false, attrs);
    }

    /**
     * Convertit du JSON en Map.
     * @param json String JSON source
     * @return Map Données extraites
     */
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

    /**
     * Convertit un objet en liste de Maps.
     * @param value Object Valeur à convertir
     * @return List Liste de Maps
     */
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

    /**
     * Copie une Map en Map String Object.
     * @param raw Map Map source
     * @return Map Map copiée
     */
    private static Map<String, Object> copyToStringObjectMap(Map<?, ?> raw) {
        Map<String, Object> copy = new LinkedHashMap<>(raw.size());
        for (Entry<?, ?> entry : raw.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return copy;
    }
}
