package com.arrendamientos.usuarios.domain.port.in;

public interface EnviarVerificacionEmailUseCase {
    void enviar(String userId, String correo);
}
