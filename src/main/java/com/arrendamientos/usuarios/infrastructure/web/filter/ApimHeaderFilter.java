package com.arrendamientos.usuarios.infrastructure.web.filter;

import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ApimHeaderFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApimHeaderFilter.class);
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final AppProperties properties;
    private final String activeProfile;
    private final boolean enabled;
    private final ObjectMapper objectMapper;

    public ApimHeaderFilter(
            AppProperties properties,
            @Value("${spring.profiles.active:dev}") String activeProfile,
            @Value("${app.security.apim-filter-enabled:true}") boolean enabled,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.activeProfile = activeProfile;
        this.enabled = enabled;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return properties.apim().skipPaths() != null
                && properties.apim().skipPaths().stream().anyMatch(p -> MATCHER.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(req, res);
            return;
        }
        boolean isDev = "dev".equalsIgnoreCase(activeProfile) || "test".equalsIgnoreCase(activeProfile);
        if (isDev && !properties.apim().validateClientCert()) {
            chain.doFilter(req, res);
            return;
        }

        String subscriptionKey = req.getHeader("Ocp-Apim-Subscription-Key");
        if (subscriptionKey == null || subscriptionKey.isBlank()) {
            writeError(res, HttpStatus.UNAUTHORIZED, "Missing Ocp-Apim-Subscription-Key header");
            return;
        }
        if (!subscriptionKey.equals(properties.apim().subscriptionKey())) {
            writeError(res, HttpStatus.FORBIDDEN, "Invalid subscription key");
            return;
        }

        if (properties.apim().validateClientCert()) {
            String cert = req.getHeader("X-ARR-ClientCert");
            if (cert == null || cert.isBlank()) {
                writeError(res, HttpStatus.FORBIDDEN, "Client certificate required");
                return;
            }
            String thumbprint = extractThumbprint(cert);
            if (thumbprint == null) {
                writeError(res, HttpStatus.BAD_REQUEST, "Invalid client certificate");
                return;
            }
            String expected = properties.apim().clientCertThumbprint();
            if (expected == null || !thumbprint.equalsIgnoreCase(expected)) {
                writeError(res, HttpStatus.FORBIDDEN, "Invalid client certificate thumbprint");
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private String extractThumbprint(String certBase64) {
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(certBase64);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(decoded);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.debug("Error extrayendo thumbprint: {}", e.getMessage());
            return null;
        }
    }

    private void writeError(HttpServletResponse res, HttpStatus status, String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
