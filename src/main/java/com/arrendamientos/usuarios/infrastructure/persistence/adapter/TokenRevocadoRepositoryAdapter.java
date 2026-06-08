package com.arrendamientos.usuarios.infrastructure.persistence.adapter;

import com.arrendamientos.usuarios.domain.port.out.TokenRevocadoRepositoryPort;
import com.arrendamientos.usuarios.infrastructure.persistence.entity.TokenRevocadoEntity;
import com.arrendamientos.usuarios.infrastructure.persistence.mapper.TokenRevocadoMapper;
import com.arrendamientos.usuarios.infrastructure.persistence.repository.TokenRevocadoJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TokenRevocadoRepositoryAdapter implements TokenRevocadoRepositoryPort {

    private final TokenRevocadoJpaRepository jpa;
    private final TokenRevocadoMapper mapper;

    public TokenRevocadoRepositoryAdapter(TokenRevocadoJpaRepository jpa, TokenRevocadoMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void revocar(String tokenId, Instant expiracion) {
        jpa.save(mapper.toEntity(tokenId, expiracion));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaRevocado(String tokenId) {
        return jpa.existsById(tokenId);
    }

    @Override
    @Transactional
    public int limpiarAntiguos(int diasAntiguedad) {
        Instant limite = Instant.now().minus(diasAntiguedad, ChronoUnit.DAYS);
        return jpa.eliminarAntiguos(limite);
    }
}
