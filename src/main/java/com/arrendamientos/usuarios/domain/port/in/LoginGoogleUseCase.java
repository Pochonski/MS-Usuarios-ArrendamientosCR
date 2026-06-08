package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.application.dto.AuthResult;
import com.arrendamientos.usuarios.application.dto.GoogleLoginCommand;

public interface LoginGoogleUseCase {
    AuthResult loginGoogle(GoogleLoginCommand cmd);
}
