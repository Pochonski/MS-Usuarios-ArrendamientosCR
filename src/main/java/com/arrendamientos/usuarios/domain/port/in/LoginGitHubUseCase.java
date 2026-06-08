package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.application.dto.AuthResult;
import com.arrendamientos.usuarios.application.dto.GitHubLoginCommand;

public interface LoginGitHubUseCase {
    AuthResult loginGitHub(GitHubLoginCommand cmd);
}
