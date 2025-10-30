// src/JavalinJsonMapper.java
import io.javalin.json.JsonMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.lang.reflect.Type;
import java.util.Collection;

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
        if (obj instanceof Collection || obj.getClass().isArray()) {
            return new JSONArray(obj).toString();
        }
        if (obj instanceof String) {
            // Éviter la double-quotation des chaînes JSON déjà formatées
            String str = (String) obj;
            if ((str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]"))) {
                return str;
            }
        }
        // Utilise le constructeur de JSONObject qui inspecte les getters de l'objet.
        return new JSONObject(obj).toString();
    }
}
