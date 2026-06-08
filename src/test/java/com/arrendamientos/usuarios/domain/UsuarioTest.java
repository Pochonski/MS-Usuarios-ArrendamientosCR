package com.arrendamientos.usuarios.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioTest {

    @Test
    void usuarioOAuthEsTrue() {
        Usuario u = buildUsuario("usr-001", null);
        assertTrue(u.esOAuth());
    }

    @Test
    void usuarioConHashNoEsOAuth() {
        Usuario u = buildUsuario("usr-001", "hash-bcrypt");
        assertFalse(u.esOAuth());
    }

    @Test
    void cuentaBloqueadaCuandoBloqueadoHastaEsFuturo() {
        Usuario u = buildUsuarioConBloqueo("usr-001", "hash", java.time.Instant.now().plusSeconds(60));
        assertTrue(u.esCuentaBloqueada(java.time.Instant.now()));
    }

    @Test
    void cuentaNoBloqueadaSiBloqueadoHastaEsPasado() {
        Usuario u = buildUsuarioConBloqueo("usr-001", "hash", java.time.Instant.now().minusSeconds(60));
        assertFalse(u.esCuentaBloqueada(java.time.Instant.now()));
    }

    @Test
    void cuentaNoBloqueadaSiBloqueadoHastaEsNulo() {
        Usuario u = buildUsuario("usr-001", "hash");
        assertFalse(u.esCuentaBloqueada(java.time.Instant.now()));
    }

    @Test
    void viewNoExponeHash() {
        Usuario u = buildUsuario("usr-001", "secreto");
        UsuarioView v = u.aView();
        assertEquals("usr-001", v.id());
        assertEquals("Juan", v.nombre());
        assertEquals("juan@example.com", v.correo());
        assertNotNull(v.rol());
    }

    @Test
    void viewConCamposNulos() {
        Usuario u = buildUsuarioSinAvatar("usr-001", null);
        UsuarioView v = u.aView();
        assertNull(v.avatar());
        assertNull(v.ultimoLogin());
    }

    @Test
    void viewMantieneRol() {
        Usuario dueno = buildUsuario("usr-001", "hash");
        UsuarioView vDueno = dueno.aView();
        assertEquals(RolUsuario.DUENO, vDueno.rol());

        Usuario inquilino = new Usuario(
                dueno.id(), dueno.nombre(), dueno.correo(), dueno.contrasenaHash(),
                RolUsuario.INQUILINO, dueno.telefono(), dueno.avatar(), dueno.googleId(),
                dueno.fechaRegistro(), dueno.ultimoLogin(), dueno.intentosFallidos(), dueno.bloqueadoHasta()
        );
        assertEquals(RolUsuario.INQUILINO, inquilino.aView().rol());
    }

    private static Usuario buildUsuario(String id, String hash) {
        return new Usuario(
                new UsuarioId(id),
                "Juan",
                "juan@example.com",
                hash == null ? null : new PasswordHash(hash),
                RolUsuario.DUENO,
                "+50688888888",
                "https://avatar.example.com/juan.png",
                null,
                java.time.Instant.parse("2024-01-01T00:00:00Z"),
                null,
                0,
                null
        );
    }

    private static Usuario buildUsuarioSinAvatar(String id, String hash) {
        return new Usuario(
                new UsuarioId(id),
                "Juan",
                "juan@example.com",
                hash == null ? null : new PasswordHash(hash),
                RolUsuario.DUENO,
                "+50688888888",
                null,
                null,
                java.time.Instant.parse("2024-01-01T00:00:00Z"),
                null,
                0,
                null
        );
    }

    private static Usuario buildUsuarioConBloqueo(String id, String hash, java.time.Instant bloqueadoHasta) {
        return new Usuario(
                new UsuarioId(id),
                "Juan",
                "juan@example.com",
                hash == null ? null : new PasswordHash(hash),
                RolUsuario.DUENO,
                "+50688888888",
                "https://avatar.example.com/juan.png",
                null,
                java.time.Instant.parse("2024-01-01T00:00:00Z"),
                null,
                5,
                bloqueadoHasta
        );
    }
}
