package com.arrendamientos.usuarios.application.service;

import com.arrendamientos.usuarios.domain.exception.CredencialesInvalidasException;
import com.arrendamientos.usuarios.domain.model.Usuario;
import com.arrendamientos.usuarios.domain.port.in.EnviarVerificacionEmailUseCase;
import com.arrendamientos.usuarios.domain.port.in.VerificarEmailUseCase;
import com.arrendamientos.usuarios.domain.port.out.EmailSenderPort;
import com.arrendamientos.usuarios.domain.port.out.TokenProviderPort;
import com.arrendamientos.usuarios.domain.port.out.UsuarioRepositoryPort;
import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailVerificationService implements VerificarEmailUseCase, EnviarVerificacionEmailUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final TokenProviderPort tokenProvider;
    private final EmailSenderPort emailSender;
    private final AppProperties properties;

    public EmailVerificationService(
            UsuarioRepositoryPort usuarios,
            TokenProviderPort tokenProvider,
            EmailSenderPort emailSender,
            AppProperties properties) {
        this.usuarios = usuarios;
        this.tokenProvider = tokenProvider;
        this.emailSender = emailSender;
        this.properties = properties;
    }

    @Override
    @Transactional(readOnly = true)
    public Resultado verificar(String token) {
        Claims claims = tokenProvider.parsearEmailVerificationToken(token);
        String userId = claims.get("userId", String.class);
        String correo = claims.get("correo", String.class);
        Usuario usuario = usuarios.porId(userId)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));
        return new Resultado(usuario.id().value(), correo);
    }

    @Override
    @Transactional(readOnly = true)
    public void enviar(String userId, String correo) {
        Usuario usuario = usuarios.porId(userId)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));
        String token = tokenProvider.generarEmailVerificationToken(usuario.id().value(), correo);
        String baseUrl = properties.emailVerification().frontendBaseUrl();
        String url = baseUrl + "/verify-email?token=" + token;
        emailSender.enviarVerificacion(correo, usuario.nombre(), url);
    }
}
