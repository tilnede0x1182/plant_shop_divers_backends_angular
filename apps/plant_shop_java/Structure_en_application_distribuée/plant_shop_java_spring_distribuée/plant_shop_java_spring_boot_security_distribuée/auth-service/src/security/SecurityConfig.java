package security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.userdetails.UserDetailsService;

@Configuration
@EnableWebSecurity
/**
 * Configuration de sécurité Spring Security.
 */
public class SecurityConfig {

    private final SessionAuthFilter sessionAuthFilter;

    @Autowired
    /**
     * Constructeur avec injection du filtre de session.
     * @param sessionAuthFilter Filtre d'authentification par session
     */
    public SecurityConfig(SessionAuthFilter sessionAuthFilter) {
        this.sessionAuthFilter = sessionAuthFilter;
    }

    @Bean
    /**
     * Configure la chaîne de filtres de sécurité.
     * @param http Configuration HTTP Security
     * @return Chaîne de filtres configurée
     * @throws Exception En cas d'erreur de configuration
     */
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
						.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .addFilterBefore(sessionAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    /**
     * Bean pour l'encodeur de mots de passe.
     * @return Encodeur BCrypt
     */
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    /**
     * Bean pour le service de détails utilisateur.
     * @return Service en mémoire vide
     */
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager();
    }
}
