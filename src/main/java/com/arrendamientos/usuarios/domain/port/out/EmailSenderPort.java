package com.arrendamientos.usuarios.domain.port.out;

public interface EmailSenderPort {
    void enviarVerificacion(String correo, String nombre, String verificationUrl);
}
