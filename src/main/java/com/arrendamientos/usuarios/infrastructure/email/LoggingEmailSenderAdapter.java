package com.arrendamientos.usuarios.infrastructure.email;

import com.arrendamientos.usuarios.domain.port.out.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSenderAdapter.class);

    @Override
    public void enviarVerificacion(String correo, String nombre, String verificationUrl) {
        log.info("""
                ╔══════════════════════════════════════════════════════════════╗
                ║  EMAIL DE VERIFICACIÓN (simulado en desarrollo)
                ╠══════════════════════════════════════════════════════════════╣
                ║  Para:  {}
                ║  Nombre: {}
                ║  URL:   {}
                ╚══════════════════════════════════════════════════════════════╝
                """, correo, nombre, verificationUrl);
    }
}
