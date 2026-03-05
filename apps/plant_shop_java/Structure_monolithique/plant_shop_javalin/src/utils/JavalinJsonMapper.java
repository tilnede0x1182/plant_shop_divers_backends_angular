package util;

// src/utils/JavalinJsonMapper.java
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.json.JsonMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Implémentation d'un JsonMapper pour Javalin utilisant la librairie org.json.
 * Gère la sérialisation et la désérialisation entre les objets Java et les chaînes JSON.
 */
public final class JavalinJsonMapper implements JsonMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Désérialise une chaîne JSON en objet.
     * @param json String JSON source
     * @param targetType Type Type cible
     * @return T Objet désérialisé
     */
    @Override
    public <T> T fromJsonString(String json, Type targetType) {
        Objects.requireNonNull(json, "Le contenu JSON ne peut pas être null");
        Objects.requireNonNull(targetType, "Le type cible est requis");

        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructType(targetType);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                "Impossible de désérialiser le JSON vers " + targetType.getTypeName(), e
            );
        }
    }

    /**
     * Sérialise un objet en chaîne JSON.
     * @param obj Object Objet à sérialiser
     * @param type Type Type de l objet
     * @return String JSON
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
     * Convertit une valeur en objet JSON.
     * @param value Object Valeur à convertir
     * @return Object JSONObject, JSONArray ou valeur primitive
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
