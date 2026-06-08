package com.arrendamientos.usuarios.infrastructure.web;

import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Health check del servicio")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final AppProperties properties;
    private final DataSource dataSource;

    public HealthController(AppProperties properties, DataSource dataSource) {
        this.properties = properties;
        this.dataSource = dataSource;
    }

    @GetMapping
    @Operation(summary = "Health check con verificación de base de datos")
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbOk = pingDatabase();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", dbOk ? "healthy" : "unhealthy");
        body.put("timestamp", Instant.now().toString());
        body.put("database", Map.of("status", dbOk ? "connected" : "disconnected"));
        Map<String, Object> google = new LinkedHashMap<>();
        String clientId = properties.google().clientId();
        boolean configured = clientId != null && !clientId.isBlank();
        google.put("configured", configured);
        google.put("clientIdPrefix", configured ? clientId.substring(0, Math.min(12, clientId.length())) : null);
        body.put("google", google);
        return ResponseEntity.status(dbOk ? 200 : 503).body(body);
    }

    private boolean pingDatabase() {
        try (Connection c = dataSource.getConnection()) {
            return c.isValid(1);
        } catch (Exception e) {
            log.debug("Health check DB ping failed: {}", e.getMessage());
            return false;
        }
    }
}
