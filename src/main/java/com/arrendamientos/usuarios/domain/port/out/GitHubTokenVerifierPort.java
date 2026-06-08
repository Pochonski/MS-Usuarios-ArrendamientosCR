package com.arrendamientos.usuarios.domain.port.out;

import com.arrendamientos.usuarios.domain.model.GitHubUserInfo;

public interface GitHubTokenVerifierPort {
    GitHubUserInfo verificar(String code, String redirectUri);
}
