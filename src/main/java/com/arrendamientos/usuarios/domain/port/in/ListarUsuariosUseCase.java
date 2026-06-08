package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.domain.model.UsuarioView;

import java.util.List;

public interface ListarUsuariosUseCase {
    /**
     * @deprecated Trunca silenciosamente a 1000 usuarios. Usar {@link #listarPaginado(int, int)} en su lugar.
     */
    @Deprecated(forRemoval = true, since = "1.0.6")
    List<UsuarioView> listarTodos();

    List<UsuarioView> listarPorRol(String rol);
    List<UsuarioView> buscarPorPrefijoCorreo(String prefijo);
    ListadoPaginado<UsuarioView> listarPaginado(int page, int size);
    long contar();

    record ListadoPaginado<T>(List<T> data, int page, int size, long total, int pages) {}
}
