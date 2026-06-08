package com.arrendamientos.usuarios.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.arrendamientos.usuarios.infrastructure.ratelimit.RateLimitFilter;
import com.arrendamientos.usuarios.infrastructure.security.JwtAuthenticationFilter;
import com.arrendamientos.usuarios.infrastructure.web.filter.ApimHeaderFilter;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final AppProperties properties;
    private final String activeProfile;

    public SecurityConfig(AppProperties properties,
                          @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.properties = properties;
        this.activeProfile = activeProfile;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(properties.bcrypt().strength());
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        boolean isDev = "dev".equals(activeProfile) || "test".equals(activeProfile);
        if (isDev) {
            config.setAllowedOriginPatterns(List.of("*"));
            config.setAllowCredentials(false);
        } else {
            config.setAllowedOrigins(properties.cors().allowedOrigins());
            config.setAllowCredentials(true);
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Content-Type",
                "Authorization",
                "ocp-apim-subscription-key",
                "x-requested-with",
                "X-ARR-ClientCert",
                "X-Refresh-Token"
        ));
        config.setExposedHeaders(List.of("Authorization"));
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ApimHeaderFilter apimHeaderFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RateLimitFilter rateLimitFilter,
            org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint,
            org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(properties.security().endpointsPublic().toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apimHeaderFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler(
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            response.setStatus(403);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("error", "Forbidden");
            body.put("message", ex.getMessage() == null ? "Acceso denegado" : ex.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }

    @Bean
    public org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint(
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return (request, response, ex) -> {
            response.setStatus(401);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
            body.put("error", "Unauthorized");
            body.put("message", ex.getMessage() == null ? "No autenticado" : ex.getMessage());
            response.getWriter().write(objectMapper.writeValueAsString(body));
        };
    }
}
