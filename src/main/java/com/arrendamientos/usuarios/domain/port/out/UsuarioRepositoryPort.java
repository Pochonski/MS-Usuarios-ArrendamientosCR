package com.arrendamientos.usuarios.domain.port.out;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.Usuario;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {

    Optional<Usuario> porId(String id);

    Optional<Usuario> porCorreo(String correo);

    Optional<Usuario> porGoogleId(String googleId);

    List<Usuario> porPrefijoCorreo(String prefijo);

    List<Usuario> porRol(RolUsuario rol);

    List<Usuario> paginado(int page, int size);

    long contar();

    boolean existeCorreoExcluyendoId(String correo, String excludeId);

    Usuario guardar(Usuario usuario);

    boolean eliminar(String id);

    void actualizarUltimoLogin(String id, Instant cuando);

    int incrementarIntentosFallidos(String id);

    void resetearIntentosFallidos(String id);
}
