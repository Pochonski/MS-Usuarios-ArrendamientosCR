package com.arrendamientos.usuarios.infrastructure.security;

import com.arrendamientos.usuarios.domain.port.out.PasswordEncoderPort;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BcryptPasswordEncoderAdapterTest {

    private final PasswordEncoderPort adapter = new BcryptPasswordEncoderAdapter(new BCryptPasswordEncoder(4));

    @Test
    void hashGeneraStringBcryptValido() {
        String hash = adapter.hash("Password123!");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$"));
    }

    @Test
    void matchesDevuelveTrueParaPasswordCorrecto() {
        String hash = adapter.hash("Password123!");
        assertTrue(adapter.matches("Password123!", hash));
    }

    @Test
    void matchesDevuelveFalseParaPasswordIncorrecto() {
        String hash = adapter.hash("Password123!");
        assertFalse(adapter.matches("otro-password", hash));
    }

    @Test
    void matchesDevuelveFalseSiHashEsNull() {
        assertFalse(adapter.matches("Password123!", null));
    }

    @Test
    void matchesDevuelveFalseSiRawEsNull() {
        assertFalse(adapter.matches(null, "$2a$10$abc"));
    }

    @Test
    void hashDistintoEnCadaLlamada() {
        String h1 = adapter.hash("Password123!");
        String h2 = adapter.hash("Password123!");
        assertFalse(h1.equals(h2), "BCrypt debe generar salts distintos por hash");
    }
}
