package com.arrendamientos.usuarios.infrastructure.security;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.port.out.TokenProviderPort;
import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProviderAdapter implements TokenProviderPort {

    private static final String CLAIM_ID = "id";
    private static final String CLAIM_CORREO = "correo";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_TIPO = "tipo";
    private static final String CLAIM_USER_ID = "userId";
    private static final String TIPO_REFRESH = "refresh";

    private final SecretKey accessKey;
    private final SecretKey emailVerifyKey;
    private final long accessExpiresSeconds;
    private final long refreshExpiresSeconds;
    private final long emailVerifyExpiresSeconds;

    public JwtTokenProviderAdapter(AppProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is required");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.accessKey = Keys.hmacShaKeyFor(secretBytes);
        this.emailVerifyKey = Keys.hmacShaKeyFor((secret + "-email-verify").getBytes(StandardCharsets.UTF_8));
        this.accessExpiresSeconds = props.jwt().expiresIn().toSeconds();
        this.refreshExpiresSeconds = props.jwt().refreshExpiresIn().toSeconds();
        this.emailVerifyExpiresSeconds = props.jwt().emailVerificationExpiresIn().toSeconds();
    }

    @Override
    public String generarAccessToken(String userId, String correo, RolUsuario rol, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti == null ? UUID.randomUUID().toString() : jti)
                .subject(userId)
                .claim(CLAIM_ID, userId)
                .claim(CLAIM_CORREO, correo)
                .claim(CLAIM_ROL, rol.getValor())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpiresSeconds)))
                .signWith(accessKey)
                .compact();
    }

    @Override
    public String generarRefreshToken(String userId, String jti) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti == null ? UUID.randomUUID().toString() : jti)
                .subject(userId)
                .claim(CLAIM_ID, userId)
                .claim(CLAIM_TIPO, TIPO_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshExpiresSeconds)))
                .signWith(accessKey)
                .compact();
    }

    @Override
    public String generarEmailVerificationToken(String userId, String correo) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_CORREO, correo)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(emailVerifyExpiresSeconds)))
                .signWith(emailVerifyKey)
                .compact();
    }

    @Override
    public Claims parsearAccessToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(accessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Token inválido o expirado", e);
        }
    }

    @Override
    public Claims parsearEmailVerificationToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(emailVerifyKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new IllegalArgumentException("El enlace de verificación ha expirado. Solicita uno nuevo.", e);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Token de verificación inválido.", e);
        }
    }
}
