package com.arrendamientos.usuarios.infrastructure.persistence.repository;

import com.arrendamientos.usuarios.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, String> {

    Optional<UsuarioEntity> findByCorreo(String correo);

    Optional<UsuarioEntity> findByGoogleId(String googleId);

    Optional<UsuarioEntity> findByGitHubId(Long gitHubId);

    List<UsuarioEntity> findByCorreoStartingWith(String prefijo);

    List<UsuarioEntity> findByRol(String rol);

    Optional<UsuarioEntity> findByCorreoAndIdNot(String correo, String id);

    @Modifying
    @Query("UPDATE UsuarioEntity u SET u.ultimoLogin = :cuando WHERE u.id = :id")
    int updateUltimoLogin(@Param("id") String id, @Param("cuando") Instant cuando);

    @Modifying
    @Query("UPDATE UsuarioEntity u SET u.intentosFallidos = COALESCE(u.intentosFallidos, 0) + 1 WHERE u.id = :id")
    int incrementarIntentosFallidos(@Param("id") String id);

    @Modifying
    @Query("UPDATE UsuarioEntity u SET u.intentosFallidos = 0, u.bloqueadoHasta = null WHERE u.id = :id")
    int resetearIntentosFallidos(@Param("id") String id);
}
