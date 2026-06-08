package com.arrendamientos.usuarios.domain.exception;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends DomainException {
    public CredencialesInvalidasException() {
        super("Credenciales inválidas", HttpStatus.UNAUTHORIZED);
    }

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje, HttpStatus.UNAUTHORIZED);
    }
}
