package com.arrendamientos.usuarios.domain.exception;

import org.springframework.http.HttpStatus;

public class PermisoDenegadoException extends DomainException {
    public PermisoDenegadoException(String mensaje) {
        super(mensaje, HttpStatus.FORBIDDEN);
    }
}
