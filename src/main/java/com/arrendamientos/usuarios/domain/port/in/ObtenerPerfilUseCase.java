package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.domain.model.UsuarioView;

public interface ObtenerPerfilUseCase {
    UsuarioView perfil(String userId);
}
