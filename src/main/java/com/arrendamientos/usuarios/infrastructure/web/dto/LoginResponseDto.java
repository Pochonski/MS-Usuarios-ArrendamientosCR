package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.application.dto.AuthResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de login/registro exitoso. Contiene el access token (24h) y el refresh token (7d) del usuario, además de los datos públicos del perfil.")
public record LoginResponseDto(
        @Schema(description = "JWT access token. Incluir como 'Authorization: Bearer <token>' en requests autenticados.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIi...")
        String token,

        @Schema(description = "JWT refresh token (7d de validez). Usar en /api/auth/refresh para obtener un nuevo access token sin re-login.",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCJ9...")
        String refreshToken,

        @Schema(description = "Datos públicos del perfil del usuario autenticado")
        UsuarioResponseDto usuario
) {
    public static LoginResponseDto from(AuthResult r) {
        return new LoginResponseDto(r.token(), r.refreshToken(), UsuarioResponseDto.from(r.user()));
    }
}
