package com.arrendamientos.usuarios.infrastructure.persistence.adapter;

import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.Usuario;
import com.arrendamientos.usuarios.domain.port.out.UsuarioRepositoryPort;
import com.arrendamientos.usuarios.infrastructure.persistence.entity.UsuarioEntity;
import com.arrendamientos.usuarios.infrastructure.persistence.mapper.UsuarioMapper;
import com.arrendamientos.usuarios.infrastructure.persistence.repository.UsuarioJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository jpa;
    private final UsuarioMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository jpa, UsuarioMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> porId(String id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> porCorreo(String correo) {
        if (correo == null) {
            return Optional.empty();
        }
        return jpa.findByCorreo(correo.trim().toLowerCase()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> porGoogleId(String googleId) {
        if (googleId == null) {
            return Optional.empty();
        }
        return jpa.findByGoogleId(googleId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> porPrefijoCorreo(String prefijo) {
        if (prefijo == null) {
            return List.of();
        }
        String escaped = prefijo.replace("%", "\\%").replace("_", "\\_");
        return jpa.findByCorreoStartingWith(escaped).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> porRol(RolUsuario rol) {
        return jpa.findByRol(rol.getValor()).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> paginado(int page, int size) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "fechaRegistro"));
        return jpa.findAll(pageable).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contar() {
        return jpa.count();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeCorreoExcluyendoId(String correo, String excludeId) {
        if (correo == null) {
            return false;
        }
        return jpa.findByCorreoAndIdNot(correo.trim().toLowerCase(), excludeId).isPresent();
    }

    @Override
    @Transactional
    public Usuario guardar(Usuario usuario) {
        UsuarioEntity entity = mapper.toEntity(usuario);
        UsuarioEntity saved = jpa.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public boolean eliminar(String id) {
        if (!jpa.existsById(id)) {
            return false;
        }
        jpa.deleteById(id);
        return true;
    }

    @Override
    @Transactional
    public void actualizarUltimoLogin(String id, Instant cuando) {
        jpa.updateUltimoLogin(id, cuando);
    }

    @Override
    @Transactional
    public int incrementarIntentosFallidos(String id) {
        // Incrementar y leer el nuevo valor. Usar JPQL para portabilidad.
        jpa.incrementarIntentosFallidos(id);
        em.flush();
        em.clear();
        return jpa.findById(id).map(UsuarioEntity::getIntentosFallidos).orElse(0);
    }

    @Override
    @Transactional
    public void resetearIntentosFallidos(String id) {
        jpa.resetearIntentosFallidos(id);
    }
}
