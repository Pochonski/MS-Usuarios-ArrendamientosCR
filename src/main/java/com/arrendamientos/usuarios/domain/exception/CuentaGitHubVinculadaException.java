package com.arrendamientos.usuarios.domain.exception;

import org.springframework.http.HttpStatus;

public class CuentaGitHubVinculadaException extends DomainException {
    public CuentaGitHubVinculadaException() {
        super("Esta cuenta ya está vinculada a otra cuenta de GitHub", HttpStatus.UNAUTHORIZED);
    }
}
