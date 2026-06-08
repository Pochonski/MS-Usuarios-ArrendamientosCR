package com.arrendamientos.usuarios.application.service;

import com.arrendamientos.usuarios.application.dto.AuthResult;
import com.arrendamientos.usuarios.application.dto.CreateUsuarioCommand;
import com.arrendamientos.usuarios.application.dto.GoogleLoginCommand;
import com.arrendamientos.usuarios.application.dto.LoginCommand;
import com.arrendamientos.usuarios.application.dto.UpdateUsuarioCommand;
import com.arrendamientos.usuarios.domain.exception.CorreoYaRegistradoException;
import com.arrendamientos.usuarios.domain.exception.CredencialesInvalidasException;
import com.arrendamientos.usuarios.domain.exception.CuentaBloqueadaException;
import com.arrendamientos.usuarios.domain.exception.CuentaGoogleVinculadaException;
import com.arrendamientos.usuarios.domain.exception.PermisoDenegadoException;
import com.arrendamientos.usuarios.domain.exception.UsuarioNoEncontradoException;
import com.arrendamientos.usuarios.domain.exception.ValidacionException;
import com.arrendamientos.usuarios.domain.model.GoogleUserInfo;
import com.arrendamientos.usuarios.domain.model.PasswordHash;
import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.Usuario;
import com.arrendamientos.usuarios.domain.model.UsuarioId;
import com.arrendamientos.usuarios.domain.model.UsuarioView;
import com.arrendamientos.usuarios.domain.port.in.ActualizarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.EliminarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.ListarUsuariosUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginGoogleUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginUseCase;
import com.arrendamientos.usuarios.domain.port.in.LogoutUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerPerfilUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.RefreshTokenUseCase;
import com.arrendamientos.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.out.GoogleTokenVerifierPort;
import com.arrendamientos.usuarios.domain.port.out.PasswordEncoderPort;
import com.arrendamientos.usuarios.domain.port.out.SequenceGeneratorPort;
import com.arrendamientos.usuarios.domain.port.out.TokenProviderPort;
import com.arrendamientos.usuarios.domain.port.out.TokenRevocadoRepositoryPort;
import com.arrendamientos.usuarios.domain.port.out.UsuarioRepositoryPort;
import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import com.arrendamientos.usuarios.infrastructure.config.AuthMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class UsuarioService implements
        RegistrarUsuarioUseCase,
        LoginUseCase,
        LoginGoogleUseCase,
        RefreshTokenUseCase,
        LogoutUseCase,
        ObtenerPerfilUseCase,
        ListarUsuariosUseCase,
        ObtenerUsuarioUseCase,
        ActualizarUsuarioUseCase,
        EliminarUsuarioUseCase {

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepositoryPort usuarios;
    private final TokenRevocadoRepositoryPort tokensRevocados;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final GoogleTokenVerifierPort googleVerifier;
    private final SequenceGeneratorPort sequenceGenerator;
    private final AppProperties properties;
    private final AuthMetrics metrics;

    public UsuarioService(
            UsuarioRepositoryPort usuarios,
            TokenRevocadoRepositoryPort tokensRevocados,
            PasswordEncoderPort passwordEncoder,
            TokenProviderPort tokenProvider,
            GoogleTokenVerifierPort googleVerifier,
            SequenceGeneratorPort sequenceGenerator,
            AppProperties properties,
            AuthMetrics metrics) {
        this.usuarios = usuarios;
        this.tokensRevocados = tokensRevocados;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.googleVerifier = googleVerifier;
        this.sequenceGenerator = sequenceGenerator;
        this.properties = properties;
        this.metrics = metrics;
    }

    // ---------------- REGISTRO ----------------

    @Override
    @Transactional
    public AuthResult registrar(CreateUsuarioCommand cmd) {
        if (cmd.contrasena() == null || cmd.contrasena().isBlank()) {
            throw new ValidacionException("La contraseña es requerida para el registro");
        }
        if (usuarios.porCorreo(cmd.correo()).isPresent()) {
            metrics.registerConflict();
            throw new CorreoYaRegistradoException();
        }
        PasswordHash hash = new PasswordHash(passwordEncoder.hash(cmd.contrasena()));
        String id = sequenceGenerator.siguienteUsuarioId();
        Instant now = Instant.now();
        Usuario nuevo = new Usuario(
                new UsuarioId(id),
                cmd.nombre(),
                cmd.correo(),
                hash,
                cmd.rol(),
                cmd.telefono(),
                null,
                null,
                now,
                null,
                0,
                null
        );
        try {
            usuarios.guardar(nuevo);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            metrics.registerConflict();
            // Race condition: UNIQUE constraint on Correo
            throw new CorreoYaRegistradoException();
        }
        Usuario guardado = usuarios.porId(id)
                .orElseThrow(() -> new ValidacionException("Error al crear usuario"));
        metrics.registerSuccess();
        return finalizarLogin(guardado);
    }

    // ---------------- LOGIN ----------------

    @Override
    @Transactional
    public AuthResult login(LoginCommand cmd) {
        String correoNorm = normalizarCorreo(cmd.correo());
        Usuario usuario = usuarios.porCorreo(correoNorm)
                .orElseThrow(() -> {
                    metrics.loginFailure();
                    return new CredencialesInvalidasException();
                });

        Instant ahora = Instant.now();
        if (usuario.esCuentaBloqueada(ahora)) {
            metrics.accountLocked();
            long minutos = minutosRestantes(usuario.bloqueadoHasta());
            throw new CuentaBloqueadaException(
                    "Cuenta bloqueada. Intenta en " + minutos + " minutos.",
                    usuario.bloqueadoHasta(),
                    usuario.intentosFallidos(),
                    0,
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }

        if (usuario.esOAuth()) {
            metrics.loginFailure();
            throw new CredencialesInvalidasException();
        }

        boolean ok = passwordEncoder.matches(cmd.contrasena(), usuario.contrasenaHash().bcrypt());
        if (!ok) {
            int intentos = usuarios.incrementarIntentosFallidos(usuario.id().value());
            int max = properties.lockout().maxAttempts();
            if (intentos >= max) {
                metrics.accountLocked();
                metrics.loginFailure();
                Instant hasta = Instant.now().plus(properties.lockout().durationMinutes(), ChronoUnit.MINUTES);
                Usuario recargado = usuarios.porId(usuario.id().value()).orElse(usuario);
                throw new CuentaBloqueadaException(
                        "Cuenta bloqueada por múltiples intentos fallidos. Intenta en "
                                + properties.lockout().durationMinutes() + " minutos.",
                        recargado.bloqueadoHasta(),
                        intentos,
                        0,
                        HttpStatus.TOO_MANY_REQUESTS
                );
            }
            metrics.loginFailure();
            throw new CredencialesInvalidasExceptionConIntentos(intentos, max - intentos);
        }

        usuarios.resetearIntentosFallidos(usuario.id().value());
        usuarios.actualizarUltimoLogin(usuario.id().value(), ahora);

        try {
            tokensRevocados.limpiarAntiguos(properties.tokenRevocation().cleanupAfterDays());
        } catch (Exception e) {
            log.warn("Cleanup de tokens revocados falló (best-effort): {}", e.getMessage());
        }

        metrics.loginSuccess();
        return finalizarLogin(usuario);
    }

    // ---------------- LOGIN GOOGLE ----------------

    @Override
    @Transactional
    public AuthResult loginGoogle(GoogleLoginCommand cmd) {
        String hd = cmd.hostedDomain() == null || cmd.hostedDomain().isBlank()
                ? properties.google().allowedDomain()
                : cmd.hostedDomain();
        GoogleUserInfo googleUser;
        try {
            googleUser = googleVerifier.verificar(cmd.googleToken(), cmd.nonce(), hd);
        } catch (Exception e) {
            metrics.googleFailure();
            throw e;
        }
        String correoNorm = normalizarCorreo(googleUser.email());

        Usuario usuario = usuarios.porGoogleId(googleUser.googleId()).orElse(null);
        if (usuario != null) {
            metrics.googleSuccess();
            return finalizarLogin(usuario);
        }

        Usuario existentePorCorreo = usuarios.porCorreo(correoNorm).orElse(null);
        if (existentePorCorreo != null) {
            if (existentePorCorreo.googleId() != null
                    && !existentePorCorreo.googleId().equals(googleUser.googleId())) {
                metrics.googleFailure();
                throw new CuentaGoogleVinculadaException();
            }
            Usuario vinculado = new Usuario(
                    existentePorCorreo.id(),
                    existentePorCorreo.nombre(),
                    existentePorCorreo.correo(),
                    existentePorCorreo.contrasenaHash(),
                    existentePorCorreo.rol(),
                    existentePorCorreo.telefono(),
                    existentePorCorreo.avatar() == null ? googleUser.picture() : existentePorCorreo.avatar(),
                    googleUser.googleId(),
                    existentePorCorreo.fechaRegistro(),
                    existentePorCorreo.ultimoLogin(),
                    existentePorCorreo.intentosFallidos(),
                    existentePorCorreo.bloqueadoHasta()
            );
            usuarios.guardar(vinculado);
            usuario = usuarios.porId(vinculado.id().value()).orElse(vinculado);
        } else {
            String id = sequenceGenerator.siguienteUsuarioId();
            Instant now = Instant.now();
            Usuario nuevo = new Usuario(
                    new UsuarioId(id),
                    googleUser.name(),
                    correoNorm,
                    null,
                    cmd.rol() == null ? RolUsuario.DUENO : cmd.rol(),
                    null,
                    googleUser.picture(),
                    googleUser.googleId(),
                    now,
                    null,
                    0,
                    null
            );
            try {
                usuarios.guardar(nuevo);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                metrics.googleFailure();
                throw new CorreoYaRegistradoException();
            }
            usuario = usuarios.porId(id).orElseThrow(() -> new ValidacionException("Error al crear usuario de Google"));
        }
        metrics.googleSuccess();
        return finalizarLogin(usuario);
    }

    // ---------------- REFRESH ----------------

    @Override
    @Transactional
    public AuthResult refresh(String userId, String refreshJti) {
        Usuario usuario = usuarios.porId(userId)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario no encontrado"));
        if (refreshJti != null) {
            tokensRevocados.revocar(refreshJti, null);
        }
        if (usuario.esCuentaBloqueada(Instant.now())) {
            metrics.accountLocked();
            long minutos = minutosRestantes(usuario.bloqueadoHasta());
            throw new CuentaBloqueadaException(
                    "Cuenta bloqueada. Intenta en " + minutos + " minutos.",
                    usuario.bloqueadoHasta(),
                    usuario.intentosFallidos(),
                    0,
                    HttpStatus.TOO_MANY_REQUESTS
            );
        }
        metrics.tokenRefresh();
        return finalizarLogin(usuario);
    }

    // ---------------- LOGOUT ----------------

    @Transactional
    public void logout(String jti, Instant expiracion) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        tokensRevocados.revocar(jti, expiracion);
        metrics.logout();
    }

    // ---------------- CONSULTAS ----------------

    @Override
    @Transactional(readOnly = true)
    public UsuarioView perfil(String userId) {
        return usuarios.porId(userId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(userId))
                .aView();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioView> listarTodos() {
        return usuarios.paginado(1, 1000).stream().map(Usuario::aView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioView> listarPorRol(String rol) {
        return usuarios.porRol(RolUsuario.desde(rol)).stream().map(Usuario::aView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioView> buscarPorPrefijoCorreo(String prefijo) {
        return usuarios.porPrefijoCorreo(prefijo).stream().map(Usuario::aView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ListadoPaginado<UsuarioView> listarPaginado(int page, int size) {
        if (page < 1) page = 1;
        if (size < 1) size = 1;
        if (size > 100) size = 100;
        long total = usuarios.contar();
        int pages = (int) Math.ceil((double) total / size);
        List<UsuarioView> data = usuarios.paginado(page, size).stream().map(Usuario::aView).toList();
        return new ListadoPaginado<>(data, page, size, total, pages);
    }

    @Override
    @Transactional(readOnly = true)
    public long contar() {
        return usuarios.contar();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioView porId(String id) {
        return usuarios.porId(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id))
                .aView();
    }

    // ---------------- ACTUALIZAR ----------------

    @Override
    @Transactional
    public UsuarioView actualizar(String id, UpdateUsuarioCommand cmd, String authUserId) {
        if (authUserId == null || !authUserId.equals(id)) {
            throw new PermisoDenegadoException("No puedes actualizar otro usuario");
        }
        Usuario usuario = usuarios.porId(id)
                .orElseThrow(() -> new UsuarioNoEncontradoException(id));
        if (cmd.correo() != null && !cmd.correo().isBlank()) {
            String nuevo = normalizarCorreo(cmd.correo());
            if (usuarios.existeCorreoExcluyendoId(nuevo, id)) {
                throw new CorreoYaRegistradoException();
            }
        }
        Usuario actualizado = new Usuario(
                usuario.id(),
                cmd.nombre() == null ? usuario.nombre() : cmd.nombre(),
                cmd.correo() == null ? usuario.correo() : normalizarCorreo(cmd.correo()),
                usuario.contrasenaHash(),
                usuario.rol(),
                cmd.telefono() == null ? usuario.telefono() : cmd.telefono(),
                cmd.avatar() == null ? usuario.avatar() : cmd.avatar(),
                usuario.googleId(),
                usuario.fechaRegistro(),
                usuario.ultimoLogin(),
                usuario.intentosFallidos(),
                usuario.bloqueadoHasta()
        );
        usuarios.guardar(actualizado);
        return usuarios.porId(id).orElse(actualizado).aView();
    }

    // ---------------- ELIMINAR ----------------

    @Override
    @Transactional
    public void eliminar(String id, String authUserId) {
        if (authUserId == null || !authUserId.equals(id)) {
            throw new PermisoDenegadoException("No puedes eliminar otro usuario");
        }
        if (!usuarios.eliminar(id)) {
            throw new UsuarioNoEncontradoException(id);
        }
    }

    // ---------------- HELPERS ----------------

    private AuthResult finalizarLogin(Usuario usuario) {
        String jtiAccess = UUID.randomUUID().toString();
        String jtiRefresh = UUID.randomUUID().toString();
        String access = tokenProvider.generarAccessToken(usuario.id().value(), usuario.correo(), usuario.rol(), jtiAccess);
        String refresh = tokenProvider.generarRefreshToken(usuario.id().value(), jtiRefresh);
        return new AuthResult(access, refresh, usuario.aView());
    }

    private static String normalizarCorreo(String raw) {
        return raw == null ? null : raw.trim().toLowerCase();
    }

    private static long minutosRestantes(Instant bloqueadoHasta) {
        if (bloqueadoHasta == null) return 0;
        long diff = bloqueadoHasta.toEpochMilli() - System.currentTimeMillis();
        return diff > 0 ? (long) Math.ceil(diff / 60000.0) : 0;
    }

    public static class CredencialesInvalidasExceptionConIntentos extends CredencialesInvalidasException {
        private final int intentosFallidos;
        private final int intentosRestantes;

        public CredencialesInvalidasExceptionConIntentos(int intentosFallidos, int intentosRestantes) {
            super("Credenciales inválidas");
            this.intentosFallidos = intentosFallidos;
            this.intentosRestantes = intentosRestantes;
        }

        public int getIntentosFallidos() { return intentosFallidos; }
        public int getIntentosRestantes() { return intentosRestantes; }
    }
}
