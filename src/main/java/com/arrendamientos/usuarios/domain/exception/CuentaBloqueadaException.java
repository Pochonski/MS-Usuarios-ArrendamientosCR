package com.arrendamientos.usuarios.domain.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public class CuentaBloqueadaException extends DomainException {

    private final Instant bloqueadoHasta;
    private final int intentosFallidos;
    private final int intentosRestantes;

    public CuentaBloqueadaException(String mensaje, Instant bloqueadoHasta, int intentosFallidos, int intentosRestantes, HttpStatus status) {
        super(mensaje, status);
        this.bloqueadoHasta = bloqueadoHasta;
        this.intentosFallidos = intentosFallidos;
        this.intentosRestantes = intentosRestantes;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public int getIntentosRestantes() {
        return intentosRestantes;
    }
}
