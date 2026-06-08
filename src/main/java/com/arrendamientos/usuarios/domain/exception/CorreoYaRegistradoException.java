package com.arrendamientos.usuarios.domain.exception;

import org.springframework.http.HttpStatus;

public class CorreoYaRegistradoException extends DomainException {
    public CorreoYaRegistradoException() {
        super("El correo electrónico ya está registrado", HttpStatus.CONFLICT);
    }
}
