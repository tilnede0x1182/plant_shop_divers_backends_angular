package util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper autour de Jackson ObjectMapper pour centraliser la configuration JSON.
 * Gère les types Java 8 (Instant, Timestamp) et ignore les propriétés inconnues.
 */
public final class JsonMapper {

    private static final ObjectMapper MAPPER;
    static {
        MAPPER = new ObjectMapper();
        // Support des types Java 8 Time (Instant, etc.)
        MAPPER.registerModule(new JavaTimeModule());
        // Pas de timestamp pour les dates, on préfère ISO-8601
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Ignore les champs inconnus lors de la lecture
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Ignore les champs nulls lors de l'écriture (optionnel, rend le JSON plus propre)
        MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private JsonMapper() {}

    public static ObjectMapper get() {
        return MAPPER;
    }

    public static String stringify(Object data) {
        try {
            return MAPPER.writeValueAsString(data);
        } catch (IOException e) {
            throw new RuntimeException("Erreur de sérialisation JSON", e);
        }
    }

    public static <T> T read(InputStream is, Class<T> clazz) {
        try {
            return MAPPER.readValue(is, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Erreur de désérialisation JSON", e);
        }
    }

    public static <T> T read(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Erreur de désérialisation JSON", e);
        }
    }
}
