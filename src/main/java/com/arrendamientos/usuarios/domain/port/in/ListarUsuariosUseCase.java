package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.domain.model.UsuarioView;

import java.util.List;

public interface ListarUsuariosUseCase {
    List<UsuarioView> listarTodos();
    List<UsuarioView> listarPorRol(String rol);
    List<UsuarioView> buscarPorPrefijoCorreo(String prefijo);
    ListadoPaginado<UsuarioView> listarPaginado(int page, int size);
    long contar();

    record ListadoPaginado<T>(List<T> data, int page, int size, long total, int pages) {}
}
