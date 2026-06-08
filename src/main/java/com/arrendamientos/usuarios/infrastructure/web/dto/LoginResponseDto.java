package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.application.dto.AuthResult;

public record LoginResponseDto(
        String token,
        String refreshToken,
        UsuarioResponseDto usuario
) {
    public static LoginResponseDto from(AuthResult r) {
        return new LoginResponseDto(r.token(), r.refreshToken(), UsuarioResponseDto.from(r.user()));
    }
}
