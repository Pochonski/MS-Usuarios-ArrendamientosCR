package com.arrendamientos.usuarios.domain.model;

public record GoogleUserInfo(
        String googleId,
        String email,
        String name,
        String picture
) {
}
