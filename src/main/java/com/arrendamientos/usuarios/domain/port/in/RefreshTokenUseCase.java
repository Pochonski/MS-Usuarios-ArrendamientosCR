package com.arrendamientos.usuarios.domain.port.in;

import com.arrendamientos.usuarios.application.dto.AuthResult;

public interface RefreshTokenUseCase {
    AuthResult refresh(String userId, String refreshJti);
}
