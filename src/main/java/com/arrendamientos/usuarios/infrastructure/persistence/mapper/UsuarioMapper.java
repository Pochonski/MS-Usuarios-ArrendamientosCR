package com.arrendamientos.usuarios.infrastructure.persistence.mapper;

import com.arrendamientos.usuarios.domain.model.PasswordHash;
import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.Usuario;
import com.arrendamientos.usuarios.domain.model.UsuarioId;
import com.arrendamientos.usuarios.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;

@Component
public class UsuarioMapper {

    public Usuario toDomain(UsuarioEntity e) {
        if (e == null) {
            return null;
        }
        return new Usuario(
                new UsuarioId(e.getId()),
                e.getNombre(),
                e.getCorreo(),
                PasswordHash.esValido(e.getContrasenaHash()) ? new PasswordHash(e.getContrasenaHash()) : null,
                RolUsuario.desde(e.getRol()),
                e.getTelefono(),
                e.getAvatar(),
                e.getGoogleId(),
                e.getFechaRegistro(),
                e.getUltimoLogin(),
                e.getIntentosFallidos(),
                e.getBloqueadoHasta()
        );
    }

    public UsuarioEntity toEntity(Usuario u) {
        UsuarioEntity e = new UsuarioEntity();
        e.setId(u.id().value());
        e.setNombre(u.nombre());
        e.setCorreo(u.correo());
        e.setContrasenaHash(u.contrasenaHash() == null ? null : u.contrasenaHash().bcrypt());
        e.setRol(u.rol().getValor());
        e.setTelefono(u.telefono());
        e.setAvatar(u.avatar());
        e.setGoogleId(u.googleId());
        e.setFechaRegistro(u.fechaRegistro() == null ? Instant.now() : u.fechaRegistro());
        e.setUltimoLogin(u.ultimoLogin());
        e.setIntentosFallidos(u.intentosFallidos());
        e.setBloqueadoHasta(u.bloqueadoHasta());
        return e;
    }

    public Instant toInstantUtc(java.time.OffsetDateTime odt) {
        return odt == null ? null : odt.toInstant();
    }

    public java.time.OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
