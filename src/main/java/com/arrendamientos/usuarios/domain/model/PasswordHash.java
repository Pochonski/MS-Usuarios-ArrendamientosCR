package com.arrendamientos.usuarios.domain.model;

import java.util.Objects;

public record PasswordHash(String bcrypt) {

    public PasswordHash {
        Objects.requireNonNull(bcrypt, "El hash de contraseña no puede ser nulo");
        if (bcrypt.isBlank()) {
            throw new IllegalArgumentException("El hash de contraseña no puede estar vacío");
        }
    }

    public static boolean esValido(String bcrypt) {
        return bcrypt != null && !bcrypt.isBlank();
    }
}
