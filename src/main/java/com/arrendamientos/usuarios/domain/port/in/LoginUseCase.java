package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.application.dto.AuthResult;
import com.arrendamientos.usuarios.application.dto.LoginCommand;

public interface LoginUseCase {
    AuthResult login(LoginCommand cmd);
}
