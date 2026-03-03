package com.kafein.discovery.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Security configuration:
 * - POST /auth      → Basic Auth only (to obtain JWT)
 * - GET /           → Public (health check)
 * - Swagger paths   → Public
 * - All others      → JWT Bearer Token required
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${auth.username}")
    private String authUsername;

    @Value("${auth.password}")
    private String authPassword;

    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    @Value("${jwt.expiration-minutes}")
    private int jwtExpirationMinutes;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // /auth uses Basic Auth — handled below with httpBasic()
                .requestMatchers("/auth").authenticated()
                // All other endpoints require JWT Bearer Token
                .anyRequest().authenticated()
            )
            // Basic Auth is ONLY used for /auth endpoint to get a token
            .httpBasic(basic -> basic
                .realmName("LLM Discovery System")
            )
            // JWT filter validates Bearer tokens for all other endpoints
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var user = User.builder()
            .username(authUsername)
            .password(passwordEncoder.encode(authPassword))
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Generates a signed JWT token for the given username.
     * Called by AuthController after successful Basic Auth.
     */
    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtExpirationMinutes, ChronoUnit.MINUTES);

        SecretKey key = Keys.hmacShaKeyFor(
            padKey(jwtSecretKey).getBytes(StandardCharsets.UTF_8)
        );

        return Jwts.builder()
            .subject(username)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    }

    private String padKey(String key) {
        if (key.length() < 32) {
            return String.format("%-32s", key).replace(' ', '0');
        }
        return key;
    }
}
