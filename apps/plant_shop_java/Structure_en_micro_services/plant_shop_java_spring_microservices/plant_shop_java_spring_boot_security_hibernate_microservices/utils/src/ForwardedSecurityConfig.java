package util;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de sécurité pour les identités transférées entre microservices.
 */
@Configuration
@ConditionalOnMissingBean(SecurityFilterChain.class)
public class ForwardedSecurityConfig {

    /**
	 * Configure la chaîne de filtres de sécurité HTTP.
	 * @param http HttpSecurity Instance de configuration de sécurité
	 * @return SecurityFilterChain Chaîne de filtres configurée
	 * @throws Exception En cas d'erreur de configuration
	 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
