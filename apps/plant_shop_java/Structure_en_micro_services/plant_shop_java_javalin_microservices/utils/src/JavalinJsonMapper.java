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

    /**
     * Désérialise une chaîne JSON vers un objet.
     * @param json Chaîne JSON
     * @param targetType Type cible
     * @return Objet désérialisé
     */
    @Override
    public <T> T fromJsonString(String json, Type targetType) {
        // La désérialisation est gérée directement dans les contrôleurs avec ctx.bodyAsClass()
        // qui utilise une autre logique interne et ne passe pas par cette méthode.
        // Pour une implémentation complète et type-safe, il faudrait utiliser Jackson ou Gson.
        throw new UnsupportedOperationException(
            "La désérialisation JSON est gérée par ctx.bodyAsClass(). " +
            "Cette méthode n'est pas implémentée car elle nécessiterait une bibliothèque " +
            "comme Jackson ou Gson pour être type-safe."
        );
    }

    /**
     * Sérialise un objet en chaîne JSON.
     * @param obj Objet à sérialiser
     * @param type Type de l'objet
     * @return Chaîne JSON
     */
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

    /**
     * Enveloppe une valeur Java en structure JSON.
     * @param value Valeur à envelopper
     * @return Structure JSON correspondante
     */
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
