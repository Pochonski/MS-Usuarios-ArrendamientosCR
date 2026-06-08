package com.arrendamientos.usuarios.domain.exception;

import org.springframework.http.HttpStatus;

public class UsuarioNoEncontradoException extends DomainException {
    public UsuarioNoEncontradoException(String id) {
        super("Usuario no encontrado: " + id, HttpStatus.NOT_FOUND);
    }
}
