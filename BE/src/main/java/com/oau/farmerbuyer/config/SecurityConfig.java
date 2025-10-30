// BE/src/main/java/com/oau/farmerbuyer/config/SecurityConfig.java
package com.oau.farmerbuyer.config;

import com.oau.farmerbuyer.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// ✨ NEW imports for CORS:
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthFilter jwtAuthFilter(@Value("${app.security.jwtSecret}") String secret) {
        return new JwtAuthFilter(secret);
    }

    // ✨ NEW: env-driven CORS bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:*}") String allowed
    ) {
        CorsConfiguration config = new CorsConfiguration();
        for (String origin : allowed.split(",")) {
            String o = origin.trim();
            if (!o.isEmpty()) config.addAllowedOriginPattern(o);
        }
        config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        // (Optional) expose headers your FE might need:
        // config.setExposedHeaders(List.of("Authorization","Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // ✅ ensure CORS is enabled
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // allow preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // static / SPA assets
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/manifest.webmanifest").permitAll()

                        // public APIs
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/crops/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/paystack/webhook").permitAll()


                        // everything else
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
