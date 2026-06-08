package com.arrendamientos.usuarios.application.dto;

import com.arrendamientos.usuarios.domain.model.UsuarioView;

public record AuthResult(
        String token,
        String refreshToken,
        UsuarioView user
) {
}
