package com.arrendamientos.usuarios.domain.model;

import java.time.Instant;
import java.util.Optional;

public record Usuario(
        UsuarioId id,
        String nombre,
        String correo,
        PasswordHash contrasenaHash,
        RolUsuario rol,
        String telefono,
        String avatar,
        String googleId,
        Long gitHubId,
        Instant fechaRegistro,
        Instant ultimoLogin,
        int intentosFallidos,
        Instant bloqueadoHasta
) {

    public boolean esCuentaBloqueada(Instant ahora) {
        return Optional.ofNullable(bloqueadoHasta)
                .map(ahora::isBefore)
                .orElse(false);
    }

    public boolean esOAuth() {
        return contrasenaHash == null;
    }

    public boolean puedeActualizarseComo(String idEditor) {
        return id != null && id.value().equals(idEditor);
    }

    public UsuarioView aView() {
        return new UsuarioView(
                id.value(),
                nombre,
                correo,
                rol,
                telefono,
                avatar,
                fechaRegistro,
                ultimoLogin
        );
    }
}
