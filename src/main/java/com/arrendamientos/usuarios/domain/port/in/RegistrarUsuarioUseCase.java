package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.application.dto.AuthResult;
import com.arrendamientos.usuarios.application.dto.CreateUsuarioCommand;

public interface RegistrarUsuarioUseCase {
    AuthResult registrar(CreateUsuarioCommand cmd);
}
