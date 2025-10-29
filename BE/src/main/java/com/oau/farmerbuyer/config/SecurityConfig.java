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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthFilter jwtAuthFilter(@Value("${app.security.jwtSecret}") String secret) {
        return new JwtAuthFilter(secret);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // allow preflight (handy if you ever call from browsers)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // static / SPA assets
                        .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/manifest.webmanifest").permitAll()

                        // public APIs
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/crops/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/listings/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/payments/**").permitAll() // webhooks/init endpoints you expose

                        // everything else requires auth
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
