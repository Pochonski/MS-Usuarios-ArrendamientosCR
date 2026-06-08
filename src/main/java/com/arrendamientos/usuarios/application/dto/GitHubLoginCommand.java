package com.arrendamientos.usuarios.application.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;

public record GitHubLoginCommand(
        String code,
        String redirectUri,
        RolUsuario rol
) {
}
