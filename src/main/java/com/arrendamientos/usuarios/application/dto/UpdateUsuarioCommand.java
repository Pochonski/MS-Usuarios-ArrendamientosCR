package com.arrendamientos.usuarios.application.dto;

public record UpdateUsuarioCommand(
        String nombre,
        String correo,
        String telefono,
        String avatar
) {
}
