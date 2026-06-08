package com.arrendamientos.usuarios.infrastructure.email;

import com.arrendamientos.usuarios.domain.port.out.EmailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adapter de fallback: solo se activa cuando {@code app.email.provider != "azure-comm"}.
 * En dev/test local loguea el email en consola en lugar de enviarlo.
 * En producción este bean NO existe (lo reemplaza {@link AzureCommunicationEmailAdapter}).
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "logging", matchIfMissing = true)
public class LoggingEmailSenderAdapter implements EmailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSenderAdapter.class);

    @Override
    public void enviarVerificacion(String correo, String nombre, String verificationUrl) {
        log.info("""
                ╔══════════════════════════════════════════════════════════════╗
                ║  EMAIL DE VERIFICACIÓN (simulado, no se envía)
                ╠══════════════════════════════════════════════════════════════╣
                ║  Para:   {}
                ║  Nombre: {}
                ║  URL:    {}
                ╚══════════════════════════════════════════════════════════════╝
                """, correo, nombre, verificationUrl);
    }
}
