package com.arrendamientos.usuarios.application.dto;

public record LoginCommand(
        String correo,
        String contrasena
) {
}
