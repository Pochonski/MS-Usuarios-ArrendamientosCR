package com.arrendamientos.usuarios.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public record Correo(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Correo {
        Objects.requireNonNull(value, "El correo no puede ser nulo");
        String normalizado = value.trim().toLowerCase();
        if (!PATTERN.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("Correo inválido: " + value);
        }
        value = normalizado;
    }

    public static Correo ofNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return new Correo(raw);
    }

    public static String normalizar(String raw) {
        return raw == null ? null : raw.trim().toLowerCase();
    }
}
