package com.vinusbank.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

/**
 * Spring Security Configuration
 * WHY WE NEED THIS: Without this class, Spring Boot blocks ALL incoming requests with a 401 Unauthorized error automatically.
 * HOW IT WORKS: This class intercepts every HTTP request hitting the server and decides if it gets through based on a whitelist.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * The master firewall configuration pipeline.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // CSRF is for stateful sessions (cookies). We use stateless JWTs, so we disable it to prevent false blocks.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Tell Spring NOT to use server-side sessions (RAM).
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // Whitelist everything under auth so users CAN actually log in!
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // Whitelist Swagger
                .anyRequest().authenticated() // Everything else requires a valid JWT
            );
            
        return http.build();
    }

    /**
     * Configures the hashing algorithm used when checking saved Database passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // We will use BCrypt to hash passwords securely
    }

    /**
     * Exposes the AuthenticationManager so we can explicitly trigger a login check inside AuthController.
     */
    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
