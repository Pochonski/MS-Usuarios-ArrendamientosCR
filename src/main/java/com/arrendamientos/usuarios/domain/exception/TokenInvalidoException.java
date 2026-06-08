package com.arrendamientos.usuarios.domain.exception;

import org.springframework.http.HttpStatus;

public class TokenInvalidoException extends DomainException {
    public TokenInvalidoException(String mensaje) {
        super(mensaje, HttpStatus.UNAUTHORIZED);
    }
}
