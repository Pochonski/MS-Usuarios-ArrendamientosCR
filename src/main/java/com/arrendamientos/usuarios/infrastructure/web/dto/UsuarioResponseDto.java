package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.UsuarioView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Datos públicos de un usuario. NO expone contrasenaHash, intentosFallidos, googleId ni bloqueadoHasta.")
public record UsuarioResponseDto(
        @Schema(description = "ID único formato 'usr-XXX'", example = "usr-001")
        String id,

        @Schema(description = "Nombre completo", example = "Carlos Ramírez")
        String nombre,

        @Schema(description = "Correo electrónico", example = "carlos.ramirez@email.com")
        String correo,

        @Schema(description = "Rol del usuario en la plataforma", example = "DUENO")
        RolUsuario rol,

        @Schema(description = "Teléfono de contacto (formato E.164 o nacional)", example = "+50688888888")
        String telefono,

        @Schema(description = "URL del avatar", example = "https://lh3.googleusercontent.com/...")
        String avatar,

        @Schema(description = "Fecha de registro del usuario", example = "2026-01-15T10:30:00Z")
        Instant fechaRegistro,

        @Schema(description = "Fecha del último login exitoso", example = "2026-06-08T05:30:00Z")
        Instant ultimoLogin
) {
    public static UsuarioResponseDto from(UsuarioView v) {
        return new UsuarioResponseDto(
                v.id(), v.nombre(), v.correo(), v.rol(),
                v.telefono(), v.avatar(), v.fechaRegistro(), v.ultimoLogin()
        );
    }
}
