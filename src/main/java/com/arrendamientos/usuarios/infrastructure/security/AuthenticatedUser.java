package com.arrendamientos.usuarios.infrastructure.security;

import com.arrendamientos.usuarios.domain.model.RolUsuario;

public record AuthenticatedUser(
        String id,
        String correo,
        RolUsuario rol
) {
}
