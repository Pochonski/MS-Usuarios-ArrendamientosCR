package com.arrendamientos.usuarios.domain.port.in;

import java.time.Instant;

public interface LogoutUseCase {
    void logout(String jti, Instant expiracion);
}
