package com.arrendamientos.usuarios.domain.port.in;

public interface VerificarEmailUseCase {
    Resultado verificar(String token);

    record Resultado(String userId, String correo) {}
}
