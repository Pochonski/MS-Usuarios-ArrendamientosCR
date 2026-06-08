package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.application.dto.UpdateUsuarioCommand;
import com.arrendamientos.usuarios.domain.model.UsuarioView;

public interface ActualizarUsuarioUseCase {
    UsuarioView actualizar(String id, UpdateUsuarioCommand cmd, String authUserId);
}
