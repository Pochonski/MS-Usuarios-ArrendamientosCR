package com.arrendamientos.usuarios.domain.port.in;

public interface EliminarUsuarioUseCase {
    void eliminar(String id, String authUserId);
}
