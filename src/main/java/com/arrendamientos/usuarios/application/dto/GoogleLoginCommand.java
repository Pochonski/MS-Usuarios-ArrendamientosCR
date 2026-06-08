package com.arrendamientos.usuarios.application.dto;

import com.arrendamientos.usuarios.domain.model.RolUsuario;

public record GoogleLoginCommand(
        String googleToken,
        RolUsuario rol,
        String nonce,
        String hostedDomain
) {
}
