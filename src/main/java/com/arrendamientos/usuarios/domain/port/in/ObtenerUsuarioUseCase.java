package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.domain.model.UsuarioView;

public interface ObtenerUsuarioUseCase {
    UsuarioView porId(String id);
}
