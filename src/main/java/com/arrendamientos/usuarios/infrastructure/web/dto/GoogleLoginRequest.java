package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank String googleToken,
        RolUsuario rol,
        String nonce,
        String hd
) {
}
