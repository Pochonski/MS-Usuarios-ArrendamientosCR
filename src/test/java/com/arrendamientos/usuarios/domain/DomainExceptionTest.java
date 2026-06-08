package com.arrendamientos.usuarios.domain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomainExceptionTest {

    @Test
    void propagaMensajeYStatus() {
        DomainException e = new DomainException("test", HttpStatus.BAD_REQUEST);
        assertEquals("test", e.getMessage());
        assertSame(HttpStatus.BAD_REQUEST, e.getStatus());
    }

    @Test
    void excepcionesEspecificasTienenStatusEsperado() {
        assertEquals(HttpStatus.NOT_FOUND, new UsuarioNoEncontradoException("x").getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, new CredencialesInvalidasException().getStatus());
        assertEquals(HttpStatus.CONFLICT, new CorreoYaRegistradoException().getStatus());
        assertEquals(HttpStatus.FORBIDDEN, new PermisoDenegadoException("x").getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, new TokenInvalidoException("x").getStatus());
        assertEquals(HttpStatus.UNAUTHORIZED, new CuentaGoogleVinculadaException().getStatus());
        assertEquals(HttpStatus.BAD_REQUEST, new ValidacionException("x").getStatus());
    }

    @Test
    void cuentaBloqueadaExponeCamposExtra() {
        CuentaBloqueadaException e = new CuentaBloqueadaException(
                "bloqueado",
                java.time.Instant.now().plusSeconds(600),
                5,
                0,
                HttpStatus.TOO_MANY_REQUESTS
        );
        assertNotNull(e.getBloqueadoHasta());
        assertEquals(5, e.getIntentosFallidos());
        assertEquals(0, e.getIntentosRestantes());
        assertSame(HttpStatus.TOO_MANY_REQUESTS, e.getStatus());
    }
}
