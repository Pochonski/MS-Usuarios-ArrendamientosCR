package com.arrendamientos.usuarios.infrastructure.ratelimit;

import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.arrendamientos.usuarios.infrastructure.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter implements Ordered {

    private static final String HEALTH_PATH = "/api/health";

    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            AppProperties properties,
            ObjectMapper objectMapper,
            @org.springframework.beans.factory.annotation.Value("${app.security.rate-limit-enabled:true}") boolean enabled) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getRequestURI();
        return HEALTH_PATH.equals(path) || path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
            throws ServletException, IOException {
        if (!enabled) {
            chain.doFilter(req, res);
            return;
        }
        String path = req.getRequestURI();
        String method = req.getMethod();

        BucketType type = classify(path, method);
        String key = keyFor(req, type);
        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket(type));
        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            long retryAfter = properties.rateLimit().windowMinutes() * 60L;
            writeRateLimitError(res, retryAfter);
        }
    }

    private Bucket newBucket(BucketType type) {
        long max = switch (type) {
            case AUTH -> properties.rateLimit().authMax();
            case READ -> properties.rateLimit().readMax();
            case WRITE -> properties.rateLimit().writeMax();
            case GENERAL -> properties.rateLimit().generalMax();
        };
        Duration window = Duration.ofMinutes(properties.rateLimit().windowMinutes());
        return Bucket.builder().addLimit(Bandwidth.builder().capacity(max).refillGreedy(max, window).build()).build();
    }

    private BucketType classify(String path, String method) {
        boolean isAuth = path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/registro")
                || path.startsWith("/api/auth/google")
                || path.startsWith("/api/auth/logout")
                || path.startsWith("/api/auth/send-verification-email");
        if (isAuth) {
            return BucketType.AUTH;
        }
        if ("GET".equalsIgnoreCase(method)) {
            return BucketType.READ;
        }
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
            return BucketType.WRITE;
        }
        return BucketType.GENERAL;
    }

    private String keyFor(HttpServletRequest req, BucketType type) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = "";
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            userId = u.id();
        }
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = req.getRemoteAddr();
        }
        return type + ":" + ip + ":" + userId;
    }

    private void writeRateLimitError(HttpServletResponse res, long retryAfter) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Too Many Requests");
        body.put("message", "Demasiadas solicitudes, intenta de nuevo más tarde");
        body.put("retryAfter", retryAfter);
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private enum BucketType { AUTH, READ, WRITE, GENERAL }
}
