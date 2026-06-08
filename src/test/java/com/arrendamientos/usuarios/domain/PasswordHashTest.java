package com.arrendamientos.usuarios.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashTest {

    @Test
    void creaHashValido() {
        PasswordHash h = new PasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        assertEquals("$2a$10$abcdefghijklmnopqrstuv", h.bcrypt());
    }

    @Test
    void rechazaNulo() {
        assertThrows(NullPointerException.class, () -> new PasswordHash(null));
    }

    @Test
    void rechazaVacio() {
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash(""));
        assertThrows(IllegalArgumentException.class, () -> new PasswordHash("   "));
    }

    @Test
    void esValidoTrueParaHashPresente() {
        assertTrue(PasswordHash.esValido("$2a$10$hash"));
    }

    @Test
    void esValidoFalseParaNuloOVacio() {
        assertFalse(PasswordHash.esValido(null));
        assertFalse(PasswordHash.esValido(""));
        assertFalse(PasswordHash.esValido("   "));
    }
}
