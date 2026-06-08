package com.arrendamientos.usuarios.domain.port.out;

import java.time.Instant;

public interface TokenRevocadoRepositoryPort {
    void revocar(String tokenId, Instant expiracion);
    boolean estaRevocado(String tokenId);
    int limpiarAntiguos(int diasAntiguedad);
}
