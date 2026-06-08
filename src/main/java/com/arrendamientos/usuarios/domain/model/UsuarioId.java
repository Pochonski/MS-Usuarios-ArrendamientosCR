package com.arrendamientos.usuarios.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record UsuarioId(String value) {

    private static final Pattern PATTERN = Pattern.compile("^usr-\\d{3,}$");

    public UsuarioId {
        Objects.requireNonNull(value, "UsuarioId no puede ser nulo");
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("UsuarioId inválido: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
