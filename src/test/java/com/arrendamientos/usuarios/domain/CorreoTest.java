package com.arrendamientos.usuarios.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorreoTest {

    @Test
    void normalizaMinusculasYTrim() {
        Correo c = new Correo("  USER@Example.COM  ");
        assertEquals("user@example.com", c.value());
    }

    @Test
    void rechazaCorreoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new Correo("no-es-correo"));
        assertThrows(IllegalArgumentException.class, () -> new Correo("@example.com"));
        assertThrows(IllegalArgumentException.class, () -> new Correo("test@"));
        assertThrows(IllegalArgumentException.class, () -> new Correo(""));
    }

    @Test
    void rechazaNulo() {
        assertThrows(NullPointerException.class, () -> new Correo(null));
    }

    @Test
    void ofNullableDevuelveNullParaNullOVacio() {
        assertTrue(Correo.ofNullable(null) == null);
        assertTrue(Correo.ofNullable("") == null);
        assertTrue(Correo.ofNullable("   ") == null);
    }

    @Test
    void ofNullableCreaParaValorValido() {
        Correo c = Correo.ofNullable("Foo@BAR.com");
        assertEquals("foo@bar.com", c.value());
    }

    @Test
    void normalizarEsHelperEstatico() {
        assertEquals("foo@bar.com", Correo.normalizar("  FOO@BAR.com  "));
        assertEquals(null, Correo.normalizar(null));
    }
}
