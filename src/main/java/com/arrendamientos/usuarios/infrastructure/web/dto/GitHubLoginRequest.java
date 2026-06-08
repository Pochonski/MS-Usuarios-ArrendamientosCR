package com.arrendamientos.usuarios.infrastructure.web.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import jakarta.validation.constraints.NotBlank;

public record GitHubLoginRequest(
        @NotBlank String code,
        @NotBlank String redirectUri,
        RolUsuario rol
) {
}
