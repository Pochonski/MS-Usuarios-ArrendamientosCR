package com.arrendamientos.usuarios.infrastructure.security;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.testsupport.TestJwt;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderAdapterTest {

    private final JwtTokenProviderAdapter provider = TestJwt.provider();

    @Test
    void accessTokenGeneraJtiCuandoNoSePasa() {
        String t1 = provider.generarAccessToken("usr-001", "j@e.com", RolUsuario.DUENO, null);
        String t2 = provider.generarAccessToken("usr-001", "j@e.com", RolUsuario.DUENO, null);
        assertNotNull(t1);
        assertNotNull(t2);
        assertTrue(!t1.equals(t2), "Cada token debe tener jti único");
    }

    @Test
    void accessTokenConJtiFijoEsVerificable() {
        String t = provider.generarAccessToken("usr-001", "j@e.com", RolUsuario.DUENO, "mi-jti-fijo");
        Claims c = provider.parsearAccessToken(t);
        assertEquals("mi-jti-fijo", c.getId());
        assertEquals("usr-001", c.get("id", String.class));
        assertEquals("j@e.com", c.get("correo", String.class));
        assertEquals("dueno", c.get("rol", String.class));
    }

    @Test
    void refreshTokenContieneTipoRefresh() {
        String t = provider.generarRefreshToken("usr-001", "jti-ref");
        Claims c = provider.parsearAccessToken(t);
        assertEquals("refresh", c.get("tipo", String.class));
        assertEquals("usr-001", c.get("id", String.class));
    }

    @Test
    void tokenInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> provider.parsearAccessToken("token.invalido.aqui"));
    }

    @Test
    void tokenConFirmaIncorrectaLanzaExcepcion() {
        String t = provider.generarAccessToken("usr-001", "j@e.com", RolUsuario.DUENO, "jti");
        String manipulado = t.substring(0, t.length() - 4) + "XXXX";
        assertThrows(IllegalArgumentException.class, () -> provider.parsearAccessToken(manipulado));
    }

    @Test
    void emailVerificationTokenEsVerificable() {
        String t = provider.generarEmailVerificationToken("usr-001", "verify@e.com");
        Claims c = provider.parsearEmailVerificationToken(t);
        assertEquals("usr-001", c.get("userId", String.class));
        assertEquals("verify@e.com", c.get("correo", String.class));
    }

    @Test
    void emailVerificationTokenConFirmaDistintaNoSeVerificaConSecretDeAccessToken() {
        String t = provider.generarEmailVerificationToken("usr-001", "v@e.com");
        // parsearEmailVerificationToken usa emailVerifyKey, mientras parsearAccessToken usa accessKey
        assertThrows(IllegalArgumentException.class, () -> provider.parsearAccessToken(t));
    }
}
