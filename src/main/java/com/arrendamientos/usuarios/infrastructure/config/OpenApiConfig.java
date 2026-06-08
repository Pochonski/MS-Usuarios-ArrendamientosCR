package com.arrendamientos.usuarios.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 * <p>
 * La version se lee dinámicamente de {@code spring.application.version} (Spring Boot
 * 3.x la expone automáticamente desde el manifest del JAR, que a su vez viene del
 * {@code <version>} de {@code pom.xml}). Single source of truth.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.version:unknown}")
    private String appVersion;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MS-Usuarios API")
                        .version(appVersion)
                        .description("Microservicio de autenticación y gestión de usuarios para Plataforma de Arrendamientos CR"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("ApiKeyAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("Ocp-Apim-Subscription-Key")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth").addList("ApiKeyAuth"));
    }
}
