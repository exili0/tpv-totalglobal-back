package com.ncortez.TPV_TotalGlobal.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ncortez.TPV_TotalGlobal.security.JwtAuthenticationFilter;

/**
 * Configuración central de seguridad: API stateless con JWT y autorización por roles.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    // API REST stateless:
    // - sin sesión de servidor
    // - cada request debe traer su JWT en Authorization: Bearer <token>
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
            // Login/recuperación: público
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/error").permitAll()

            // Administración estricta
                        .requestMatchers("/api/users/**", "/api/stock/**").hasRole("ADMIN")

            // Catálogo/menú operativo (lectura): cualquier usuario autenticado
                        .requestMatchers(HttpMethod.GET, "/api/products/active", "/api/products/category/**", "/api/products/barcode/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/categories/active", "/api/categories/roots").authenticated()

            // Mantenimiento catálogo (alta/edición/baja): solo ADMIN
                        .requestMatchers("/api/products/**", "/api/categories/**").hasRole("ADMIN")

            // Operativa de sala/cobros/mesas: usuario autenticado
                        .requestMatchers("/api/pos/**", "/api/tables/**").authenticated()
                        .anyRequest().authenticated()
                )
        // El filtro JWT se ejecuta antes del filtro estándar de usuario/clave.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt protege contraseñas frente a fuga de base de datos.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Durante desarrollo permitimos Angular en localhost:4200.
        // En producción debe apuntar al dominio real del front.
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Idempotency-Key", "X-Client-Attempt-At"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
