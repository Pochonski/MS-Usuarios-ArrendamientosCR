package com.arrendamientos.usuarios.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioIdTest {

    @Test
    void aceptaFormatoValido() {
        assertEquals("usr-001", new UsuarioId("usr-001").value());
        assertEquals("usr-999", new UsuarioId("usr-999").value());
        assertEquals("usr-12345", new UsuarioId("usr-12345").value());
    }

    @Test
    void rechazaFormatoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new UsuarioId("user-1"));
        assertThrows(IllegalArgumentException.class, () -> new UsuarioId("usr-1"));     // 1 dígito
        assertThrows(IllegalArgumentException.class, () -> new UsuarioId("usr-12"));    // 2 dígitos
        assertThrows(IllegalArgumentException.class, () -> new UsuarioId("usr-abc"));   // letras
        assertThrows(IllegalArgumentException.class, () -> new UsuarioId("USR-001"));   // mayúsculas
        assertThrows(IllegalArgumentException.class, () -> new UsuarioId(""));
    }

    @Test
    void rechazaNulo() {
        assertThrows(NullPointerException.class, () -> new UsuarioId(null));
    }

    @Test
    void toStringDevuelveValue() {
        assertEquals("usr-042", new UsuarioId("usr-042").toString());
    }

    @Test
    void recordsIgualesParaMismoValue() {
        assertTrue(new UsuarioId("usr-001").equals(new UsuarioId("usr-001")));
    }
}
