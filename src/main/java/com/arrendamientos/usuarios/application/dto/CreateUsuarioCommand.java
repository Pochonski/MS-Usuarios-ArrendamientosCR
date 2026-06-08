package com.arrendamientos.usuarios.application.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;

public record CreateUsuarioCommand(
        String nombre,
        String correo,
        String contrasena,
        RolUsuario rol,
        String telefono
) {
}
