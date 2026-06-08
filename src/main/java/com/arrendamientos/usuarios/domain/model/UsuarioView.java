package com.arrendamientos.usuarios.domain.model;

import java.time.Instant;

public record UsuarioView(
        String id,
        String nombre,
        String correo,
        RolUsuario rol,
        String telefono,
        String avatar,
        Instant fechaRegistro,
        Instant ultimoLogin
) {
}
