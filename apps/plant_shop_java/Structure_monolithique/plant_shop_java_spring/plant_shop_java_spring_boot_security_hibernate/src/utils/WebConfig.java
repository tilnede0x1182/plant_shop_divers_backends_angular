package utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Configuration Spring MVC pour la sérialisation JSON.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure le convertisseur JSON (Jackson) globalement.
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Ajoute le support pour java.time (Instant, LocalDate, etc.)
        objectMapper.registerModule(new JavaTimeModule());

        // S'assure que les Timestamps sont sérialisés en ISO-8601 (comme "2025-11-01T10:00:00Z")
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Crée le convertisseur
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);

        // Ajoute le convertisseur à Spring
        converters.add(jacksonConverter);
    }

    /**
     * Crée le listener pour le contexte de requête.
     * @return Le RequestContextListener
     */
    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }
}
