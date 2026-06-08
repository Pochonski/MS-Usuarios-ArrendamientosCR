package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.UsuarioView;

import java.time.Instant;

public record UsuarioResponseDto(
        String id,
        String nombre,
        String correo,
        RolUsuario rol,
        String telefono,
        String avatar,
        Instant fechaRegistro,
        Instant ultimoLogin
) {
    public static UsuarioResponseDto from(UsuarioView v) {
        return new UsuarioResponseDto(
                v.id(), v.nombre(), v.correo(), v.rol(),
                v.telefono(), v.avatar(), v.fechaRegistro(), v.ultimoLogin()
        );
    }
}
