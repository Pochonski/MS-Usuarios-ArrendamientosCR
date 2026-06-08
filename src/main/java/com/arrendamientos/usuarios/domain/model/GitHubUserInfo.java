package com.arrendamientos.usuarios.domain.model;

public record GitHubUserInfo(
        Long githubId,
        String login,
        String email,
        String name,
        String avatar
) {
}
