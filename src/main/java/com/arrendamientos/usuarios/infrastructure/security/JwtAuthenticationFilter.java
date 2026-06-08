package com.arrendamientos.usuarios.infrastructure.security;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.port.out.TokenProviderPort;
import com.arrendamientos.usuarios.domain.port.out.TokenRevocadoRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProviderPort tokenProvider;
    private final TokenRevocadoRepositoryPort tokensRevocados;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            TokenProviderPort tokenProvider,
            TokenRevocadoRepositoryPort tokensRevocados,
            ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.tokensRevocados = tokensRevocados;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        String token = header.substring(7);
        try {
            Claims claims = tokenProvider.parsearAccessToken(token);
            String jti = claims.getId();
            if (jti != null && tokensRevocados.estaRevocado(jti)) {
                writeError(res, HttpStatus.UNAUTHORIZED, "Token revocado");
                return;
            }
            String id = claims.get("id", String.class);
            String correo = claims.get("correo", String.class);
            String rolStr = claims.get("rol", String.class);
            if (id == null || rolStr == null) {
                writeError(res, HttpStatus.UNAUTHORIZED, "Token inválido");
                return;
            }
            RolUsuario rol = RolUsuario.desde(rolStr);
            AuthenticatedUser principal = new AuthenticatedUser(id, correo, rol);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()))
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(auth);
            chain.doFilter(req, res);
        } catch (Exception e) {
            writeError(res, HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
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
