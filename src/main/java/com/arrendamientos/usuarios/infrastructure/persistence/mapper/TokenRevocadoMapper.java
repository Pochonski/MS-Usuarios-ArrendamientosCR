package com.arrendamientos.usuarios.infrastructure.persistence.mapper;

import com.arrendamientos.usuarios.infrastructure.persistence.entity.TokenRevocadoEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TokenRevocadoMapper {

    public TokenRevocadoEntity toEntity(String tokenId, Instant expiracion) {
        TokenRevocadoEntity e = new TokenRevocadoEntity();
        e.setTokenId(tokenId);
        e.setRevocadoEl(Instant.now());
        e.setExpiracion(expiracion);
        return e;
    }
}
