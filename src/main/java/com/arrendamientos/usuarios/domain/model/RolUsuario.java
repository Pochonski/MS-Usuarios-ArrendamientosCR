package com.arrendamientos.usuarios.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RolUsuario {
    DUENO("dueno"),
    INQUILINO("inquilino");

    private final String valor;

    RolUsuario(String valor) {
        this.valor = valor;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @JsonCreator
    public static RolUsuario desde(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("El rol no puede ser nulo");
        }
        return switch (valor.trim().toLowerCase()) {
            case "dueno" -> DUENO;
            case "inquilino" -> INQUILINO;
            default -> throw new IllegalArgumentException("Rol inválido: " + valor);
        };
    }
}
