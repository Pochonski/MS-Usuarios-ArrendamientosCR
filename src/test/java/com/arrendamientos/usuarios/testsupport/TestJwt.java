package com.arrendamientos.usuarios.testsupport;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import com.arrendamientos.usuarios.infrastructure.security.JwtTokenProviderAdapter;
import io.jsonwebtoken.Claims;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Helper para emitir tokens JWT válidos en tests.
 * Usa la misma secret que application.yml de test.
 */
public final class TestJwt {

    public static final String SECRET = "test_secret_for_jest_12345678901234567890";

    private TestJwt() {}

    public static JwtTokenProviderAdapter provider() {
        return new JwtTokenProviderAdapter(new AppProperties(
                new AppProperties.Jwt(SECRET, Duration.ofHours(1), Duration.ofDays(7), Duration.ofHours(24)),
                new AppProperties.Apim("", "", false, "", List.of()),
                new AppProperties.Google("", ""),
                new AppProperties.EmailVerification(""),
                new AppProperties.RateLimit(15, 5, 200, 50, 100),
                new AppProperties.Cors(List.of("*")),
                new AppProperties.Lockout(5, 15),
                new AppProperties.TokenRevocation(7),
                new AppProperties.Bcrypt(4),
                new AppProperties.Security(List.of()),
                new AppProperties.Email("logging", "", "test@example.com", "Test"),
                new AppProperties.Refresh(false)
        ));
    }

    public static String accessToken(String userId, String correo, RolUsuario rol) {
        return provider().generarAccessToken(userId, correo, rol, UUID.randomUUID().toString());
    }

    public static String accessTokenWithJti(String userId, String correo, RolUsuario rol, String jti) {
        return provider().generarAccessToken(userId, correo, rol, jti);
    }

    public static String refreshToken(String userId, String jti) {
        return provider().generarRefreshToken(userId, jti);
    }

    public static String expiredToken(String userId) {
        return io.jsonwebtoken.Jwts.builder()
                .subject(userId)
                .claim("id", userId)
                .claim("rol", "dueno")
                .issuedAt(new Date(System.currentTimeMillis() - 7200_000))
                .expiration(new Date(System.currentTimeMillis() - 3600_000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public static Claims parse(String token) {
        return provider().parsearAccessToken(token);
    }
}
