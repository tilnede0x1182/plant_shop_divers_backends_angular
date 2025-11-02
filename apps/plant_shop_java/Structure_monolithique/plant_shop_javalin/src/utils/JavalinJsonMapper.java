package util;

// src/utils/JavalinJsonMapper.java
import io.javalin.json.JsonMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

/**
 * Implémentation d'un JsonMapper pour Javalin utilisant la librairie org.json.
 * Gère la sérialisation et la désérialisation entre les objets Java et les chaînes JSON.
 */
public final class JavalinJsonMapper implements JsonMapper {

    @Override
    public <T> T fromJsonString(String json, Type targetType) {
        // La désérialisation est gérée directement dans les contrôleurs avec ctx.bodyAsClass()
        // qui utilise une autre logique interne. Cette méthode est donc moins critique ici.
        // Pour une implémentation complète, il faudrait utiliser une librairie comme Jackson ou Gson.
        // Ici, on se contente de retourner l'objet JSON brut pour les cas simples.
        if (json.trim().startsWith("[")) {
            return (T) new JSONArray(json);
        }
        return (T) new JSONObject(json);
    }

    @Override
    public String toJsonString(Object obj, Type type) {
        Object normalized = wrap(obj);
        if (normalized instanceof JSONObject jsonObject) {
            return jsonObject.toString();
        }
        if (normalized instanceof JSONArray jsonArray) {
            return jsonArray.toString();
        }
        return String.valueOf(normalized);
    }

    private Object wrap(Object value) {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (value instanceof JSONObject || value instanceof JSONArray) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            JSONObject json = new JSONObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                json.put(key, wrap(entry.getValue()));
            }
            return json;
        }
        if (value instanceof Collection<?> collection) {
            JSONArray array = new JSONArray();
            for (Object element : collection) {
                array.put(wrap(element));
            }
            return array;
        }
        if (value.getClass().isArray()) {
            JSONArray array = new JSONArray();
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                array.put(wrap(Array.get(value, i)));
            }
            return array;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        return new JSONObject(value);
    }
}
