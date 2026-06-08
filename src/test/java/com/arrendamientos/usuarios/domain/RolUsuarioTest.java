package com.arrendamientos.usuarios.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RolUsuarioTest {

    @Test
    void desdeConvierteMinusculas() {
        assertEquals(RolUsuario.DUENO, RolUsuario.desde("DUENO"));
        assertEquals(RolUsuario.INQUILINO, RolUsuario.desde("Inquilino"));
    }

    @Test
    void desdeLanzaExcepcionParaValorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> RolUsuario.desde("admin"));
        assertThrows(IllegalArgumentException.class, () -> RolUsuario.desde(""));
    }

    @Test
    void desdeLanzaExcepcionParaNulo() {
        assertThrows(IllegalArgumentException.class, () -> RolUsuario.desde(null));
    }

    @Test
    void valorEsStringOriginal() {
        assertEquals("dueno", RolUsuario.DUENO.getValor());
        assertEquals("inquilino", RolUsuario.INQUILINO.getValor());
    }
}
