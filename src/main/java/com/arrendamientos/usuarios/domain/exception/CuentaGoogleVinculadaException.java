package com.arrendamientos.usuarios.domain.exception;

import org.springframework.http.HttpStatus;

public class CuentaGoogleVinculadaException extends DomainException {
    public CuentaGoogleVinculadaException() {
        super("Esta cuenta ya está vinculada a otra cuenta de Google", HttpStatus.UNAUTHORIZED);
    }
}
