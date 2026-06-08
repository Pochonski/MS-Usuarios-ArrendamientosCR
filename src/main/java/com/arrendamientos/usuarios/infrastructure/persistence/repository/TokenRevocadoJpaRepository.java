package com.arrendamientos.usuarios.infrastructure.persistence.repository;

import com.arrendamientos.usuarios.infrastructure.persistence.entity.TokenRevocadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TokenRevocadoJpaRepository extends JpaRepository<TokenRevocadoEntity, String> {

    @Modifying
    @Query("DELETE FROM TokenRevocadoEntity t " +
            "WHERE (t.expiracion IS NOT NULL AND t.expiracion < :limite) " +
            "   OR (t.expiracion IS NULL AND t.revocadoEl < :limite)")
    int eliminarAntiguos(@Param("limite") Instant limite);
}
