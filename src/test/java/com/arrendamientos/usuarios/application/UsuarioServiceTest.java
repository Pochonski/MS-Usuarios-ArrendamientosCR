package com.arrendamientos.usuarios.application.service;

import com.arrendamientos.usuarios.application.dto.AuthResult;
import com.arrendamientos.usuarios.application.dto.CreateUsuarioCommand;
import com.arrendamientos.usuarios.application.dto.GitHubLoginCommand;
import com.arrendamientos.usuarios.application.dto.GoogleLoginCommand;
import com.arrendamientos.usuarios.application.dto.LoginCommand;
import com.arrendamientos.usuarios.application.dto.UpdateUsuarioCommand;
import com.arrendamientos.usuarios.domain.exception.CorreoYaRegistradoException;
import com.arrendamientos.usuarios.domain.exception.CredencialesInvalidasException;
import com.arrendamientos.usuarios.domain.exception.CuentaBloqueadaException;
import com.arrendamientos.usuarios.domain.exception.CuentaGitHubVinculadaException;
import com.arrendamientos.usuarios.domain.exception.CuentaGoogleVinculadaException;
import com.arrendamientos.usuarios.domain.exception.PermisoDenegadoException;
import com.arrendamientos.usuarios.domain.exception.UsuarioNoEncontradoException;
import com.arrendamientos.usuarios.domain.exception.ValidacionException;
import com.arrendamientos.usuarios.domain.model.GitHubUserInfo;
import com.arrendamientos.usuarios.domain.model.GoogleUserInfo;
import com.arrendamientos.usuarios.domain.model.PasswordHash;
import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.Usuario;
import com.arrendamientos.usuarios.domain.model.UsuarioId;
import com.arrendamientos.usuarios.domain.model.UsuarioView;
import com.arrendamientos.usuarios.domain.port.out.GitHubTokenVerifierPort;
import com.arrendamientos.usuarios.domain.port.out.GoogleTokenVerifierPort;
import com.arrendamientos.usuarios.domain.port.out.PasswordEncoderPort;
import com.arrendamientos.usuarios.domain.port.out.SequenceGeneratorPort;
import com.arrendamientos.usuarios.domain.port.out.TokenProviderPort;
import com.arrendamientos.usuarios.domain.port.out.TokenRevocadoRepositoryPort;
import com.arrendamientos.usuarios.domain.port.out.UsuarioRepositoryPort;
import com.arrendamientos.usuarios.infrastructure.config.AppProperties;
import com.arrendamientos.usuarios.infrastructure.config.AuthMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private UsuarioRepositoryPort usuarios;
    private TokenRevocadoRepositoryPort tokensRevocados;
    private PasswordEncoderPort passwordEncoder;
    private TokenProviderPort tokenProvider;
    private GoogleTokenVerifierPort googleVerifier;
    private GitHubTokenVerifierPort gitHubVerifier;
    private SequenceGeneratorPort sequenceGenerator;
    private AppProperties properties;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        usuarios = mock(UsuarioRepositoryPort.class);
        tokensRevocados = mock(TokenRevocadoRepositoryPort.class);
        passwordEncoder = mock(PasswordEncoderPort.class);
        tokenProvider = mock(TokenProviderPort.class);
        googleVerifier = mock(GoogleTokenVerifierPort.class);
        gitHubVerifier = mock(GitHubTokenVerifierPort.class);
        sequenceGenerator = mock(SequenceGeneratorPort.class);
        properties = new AppProperties(
                new AppProperties.Jwt("secret", Duration.ofHours(1), Duration.ofDays(7), Duration.ofHours(24)),
                new AppProperties.Apim("", "", false, "", List.of()),
                new AppProperties.Google("", ""),
                new AppProperties.GitHub("", ""),
                new AppProperties.EmailVerification(""),
                new AppProperties.RateLimit(15, 5, 200, 50, 100),
                new AppProperties.Cors(List.of("*")),
                new AppProperties.Lockout(5, 15),
                new AppProperties.TokenRevocation(7),
                new AppProperties.Bcrypt(10),
                new AppProperties.Security(List.of())
        );
        service = new UsuarioService(usuarios, tokensRevocados, passwordEncoder, tokenProvider, googleVerifier, gitHubVerifier, sequenceGenerator, properties, new AuthMetrics(new SimpleMeterRegistry()));

        lenient().when(tokenProvider.generarAccessToken(anyString(), anyString(), any(), anyString())).thenReturn("access.token");
        lenient().when(tokenProvider.generarRefreshToken(anyString(), anyString())).thenReturn("refresh.token");
        lenient().when(passwordEncoder.hash(anyString())).thenReturn("$2a$10$hashed");
        lenient().when(passwordEncoder.matches(eq("Password123!"), anyString())).thenReturn(true);
        lenient().when(passwordEncoder.matches(eq("wrong"), anyString())).thenReturn(false);
        lenient().when(tokensRevocados.limpiarAntiguos(anyInt())).thenReturn(0);
    }

    private static Usuario buildUsuario(String id, String hash) {
        return new Usuario(
                new UsuarioId(id),
                "Juan",
                "juan@example.com",
                hash == null ? null : new PasswordHash(hash),
                RolUsuario.DUENO,
                "+50688888888",
                null,
                null,
                null,
                Instant.parse("2024-01-01T00:00:00Z"),
                null,
                0,
                null
        );
    }

    private static Usuario buildUsuarioBloqueado(String id) {
        return new Usuario(
                new UsuarioId(id),
                "Juan",
                "juan@example.com",
                new PasswordHash("$2a$10$hashed"),
                RolUsuario.DUENO,
                "+50688888888",
                null, null, null,
                Instant.parse("2024-01-01T00:00:00Z"),
                null,
                5,
                Instant.now().plus(15, ChronoUnit.MINUTES)
        );
    }

    // ---------------- REGISTRO ----------------

    @Nested
    @DisplayName("registrar")
    class Registrar {

        @Test
        void registroExitosoDevuelveAuthResultConTokens() {
            when(usuarios.porCorreo("juan@example.com")).thenReturn(Optional.empty());
            when(sequenceGenerator.siguienteUsuarioId()).thenReturn("usr-042");
            when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarios.porId("usr-042")).thenReturn(Optional.of(buildUsuario("usr-042", "$2a$10$hashed")));

            // El controller normaliza ANTES de invocar el use case
            CreateUsuarioCommand cmd = new CreateUsuarioCommand(
                    "Juan", "juan@example.com", "Password123!", RolUsuario.DUENO, "+50688888888");
            AuthResult r = service.registrar(cmd);

            assertNotNull(r);
            assertEquals("access.token", r.token());
            assertEquals("refresh.token", r.refreshToken());
            assertEquals("usr-042", r.user().id());
            assertEquals("juan@example.com", r.user().correo());
            verify(usuarios).porCorreo("juan@example.com");
            verify(usuarios, times(1)).guardar(any(Usuario.class));
        }

        @Test
        void registroConContrasenaVaciaLanzaValidacion() {
            CreateUsuarioCommand cmd = new CreateUsuarioCommand("Juan", "j@e.com", "", RolUsuario.DUENO, null);
            assertThrows(ValidacionException.class, () -> service.registrar(cmd));
            verify(usuarios, never()).guardar(any());
        }

        @Test
        void registroCorreoExistenteLanza409() {
            when(usuarios.porCorreo("juan@example.com")).thenReturn(Optional.of(buildUsuario("usr-001", "hash")));
            CreateUsuarioCommand cmd = new CreateUsuarioCommand("Juan", "juan@example.com", "Password123!", RolUsuario.DUENO, null);
            assertThrows(CorreoYaRegistradoException.class, () -> service.registrar(cmd));
            verify(usuarios, never()).guardar(any());
        }

        @Test
        void registroConUniqueConstraintRaceConditionLanza409() {
            when(usuarios.porCorreo("juan@example.com")).thenReturn(Optional.empty());
            when(sequenceGenerator.siguienteUsuarioId()).thenReturn("usr-043");
            when(usuarios.guardar(any(Usuario.class)))
                    .thenThrow(new DataIntegrityViolationException("UNIQUE KEY violation"));

            CreateUsuarioCommand cmd = new CreateUsuarioCommand("Juan", "juan@example.com", "Password123!", RolUsuario.DUENO, null);
            assertThrows(CorreoYaRegistradoException.class, () -> service.registrar(cmd));
        }
    }

    // ---------------- LOGIN ----------------

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        void loginExitosoNormalizaCorreoYActualizaUltimoLogin() {
            when(usuarios.porCorreo("juan@example.com")).thenReturn(Optional.of(buildUsuario("usr-001", "$2a$10$hashed")));
            AuthResult r = service.login(new LoginCommand("  Juan@Example.COM  ", "Password123!"));
            assertNotNull(r);
            verify(usuarios).porCorreo("juan@example.com");
            verify(usuarios).resetearIntentosFallidos("usr-001");
            verify(usuarios).actualizarUltimoLogin(eq("usr-001"), any());
        }

        @Test
        void loginUsuarioNoExisteLanzaCredencialesInvalidas() {
            when(usuarios.porCorreo("nadie@example.com")).thenReturn(Optional.empty());
            assertThrows(CredencialesInvalidasException.class,
                    () -> service.login(new LoginCommand("nadie@example.com", "x")));
        }

        @Test
        void loginUsuarioOAuthSinPasswordLanzaCredencialesInvalidas() {
            when(usuarios.porCorreo("oauth@example.com")).thenReturn(Optional.of(buildUsuario("usr-001", null)));
            assertThrows(CredencialesInvalidasException.class,
                    () -> service.login(new LoginCommand("oauth@example.com", "Password123!")));
        }

        @Test
        void loginConContrasenaIncorrectaIncrementaIntentos() {
            when(usuarios.porCorreo("juan@example.com")).thenReturn(Optional.of(buildUsuario("usr-001", "$2a$10$hashed")));
            when(usuarios.incrementarIntentosFallidos("usr-001")).thenReturn(1);

            UsuarioService.CredencialesInvalidasExceptionConIntentos ex = assertThrows(
                    UsuarioService.CredencialesInvalidasExceptionConIntentos.class,
                    () -> service.login(new LoginCommand("juan@example.com", "wrong"))
            );
            assertEquals(1, ex.getIntentosFallidos());
            assertEquals(4, ex.getIntentosRestantes());
            verify(usuarios).incrementarIntentosFallidos("usr-001");
        }

        @Test
        void loginConCincoIntentosFallidosLanzaCuentaBloqueada() {
            when(usuarios.porCorreo("juan@example.com")).thenReturn(Optional.of(buildUsuario("usr-001", "$2a$10$hashed")));
            when(usuarios.incrementarIntentosFallidos("usr-001")).thenReturn(5);
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(buildUsuarioBloqueado("usr-001")));

            CuentaBloqueadaException ex = assertThrows(CuentaBloqueadaException.class,
                    () -> service.login(new LoginCommand("juan@example.com", "wrong")));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
            assertEquals(5, ex.getIntentosFallidos());
        }

        @Test
        void loginConCuentaYaBloqueadaLanza429SinConsultarPassword() {
            when(usuarios.porCorreo("juan@example.com")).thenReturn(Optional.of(buildUsuarioBloqueado("usr-001")));
            CuentaBloqueadaException ex = assertThrows(CuentaBloqueadaException.class,
                    () -> service.login(new LoginCommand("juan@example.com", "Password123!")));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }
    }

    // ---------------- LOGIN GOOGLE ----------------

    @Nested
    @DisplayName("loginGoogle")
    class LoginGoogle {

        private final GoogleUserInfo googleUser = new GoogleUserInfo("google-sub-1", "g@example.com", "G User", "https://avatar.png");

        @Test
        void usuarioGoogleExistenteLogeaSinConsultarPorCorreo() {
            Usuario u = buildUsuario("usr-001", null);
            when(googleVerifier.verificar(eq("tok"), any(), any())).thenReturn(googleUser);
            when(usuarios.porGoogleId("google-sub-1")).thenReturn(Optional.of(u));

            AuthResult r = service.loginGoogle(new GoogleLoginCommand("tok", null, null, null));
            assertNotNull(r);
            verify(usuarios, never()).porCorreo(anyString());
        }

        @Test
        void vinculaGoogleIdAUsuarioConCorreoExistente() {
            Usuario sinGoogle = buildUsuario("usr-001", "$2a$10$hashed");
            when(googleVerifier.verificar(eq("tok"), any(), any())).thenReturn(googleUser);
            when(usuarios.porGoogleId("google-sub-1")).thenReturn(Optional.empty());
            when(usuarios.porCorreo("g@example.com")).thenReturn(Optional.of(sinGoogle));
            when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(sinGoogle));

            AuthResult r = service.loginGoogle(new GoogleLoginCommand("tok", null, null, null));
            assertNotNull(r);
            verify(usuarios).guardar(any(Usuario.class));
        }

        @Test
        void rechazaTakeoverCuandoGoogleIdYaExiste() {
            Usuario base = buildUsuario("usr-001", "$2a$10$hashed");
            // Forzar takeover: usuario con GoogleId distinto
            Usuario conOtroGoogle = new Usuario(
                    base.id(), base.nombre(), base.correo(),
                    base.contrasenaHash(), base.rol(), base.telefono(),
                    base.avatar(),
                    "otro-google-id",
                    base.gitHubId(),
                    base.fechaRegistro(), base.ultimoLogin(),
                    base.intentosFallidos(), base.bloqueadoHasta()
            );
            when(googleVerifier.verificar(eq("tok"), any(), any())).thenReturn(googleUser);
            when(usuarios.porGoogleId("google-sub-1")).thenReturn(Optional.empty());
            when(usuarios.porCorreo("g@example.com")).thenReturn(Optional.of(conOtroGoogle));
            lenient().when(usuarios.porId("usr-001")).thenReturn(Optional.of(conOtroGoogle));

            assertThrows(CuentaGoogleVinculadaException.class,
                    () -> service.loginGoogle(new GoogleLoginCommand("tok", null, null, null)));
        }

        @Test
        void creaUsuarioNuevoGoogle() {
            when(googleVerifier.verificar(eq("tok"), any(), any())).thenReturn(googleUser);
            when(usuarios.porGoogleId("google-sub-1")).thenReturn(Optional.empty());
            when(usuarios.porCorreo("g@example.com")).thenReturn(Optional.empty());
            when(sequenceGenerator.siguienteUsuarioId()).thenReturn("usr-077");
            when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarios.porId("usr-077")).thenReturn(Optional.of(
                    new Usuario(new UsuarioId("usr-077"), "G User", "g@example.com", null, RolUsuario.INQUILINO, null, "https://avatar.png", "google-sub-1", null, Instant.now(), null, 0, null)
            ));

            AuthResult r = service.loginGoogle(new GoogleLoginCommand("tok", RolUsuario.INQUILINO, null, null));
            assertNotNull(r);
            verify(usuarios).guardar(any(Usuario.class));
            verify(sequenceGenerator).siguienteUsuarioId();
        }

        @Test
        void creaUsuarioGooglePorDefaultComoDueno() {
            when(googleVerifier.verificar(eq("tok"), any(), any())).thenReturn(googleUser);
            when(usuarios.porGoogleId(any())).thenReturn(Optional.empty());
            when(usuarios.porCorreo(any())).thenReturn(Optional.empty());
            when(sequenceGenerator.siguienteUsuarioId()).thenReturn("usr-078");
            when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarios.porId("usr-078")).thenReturn(Optional.of(
                    new Usuario(new UsuarioId("usr-078"), "G", "g@e.com", null, RolUsuario.DUENO, null, null, "google-sub-1", null, Instant.now(), null, 0, null)
            ));

            AuthResult r = service.loginGoogle(new GoogleLoginCommand("tok", null, null, null));
            assertNotNull(r);
        }

        @Test
        void noConsultaRepositorioSiGoogleFalla() {
            when(googleVerifier.verificar(eq("bad"), any(), any())).thenThrow(new IllegalArgumentException("bad token"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.loginGoogle(new GoogleLoginCommand("bad", null, null, null)));
            verify(usuarios, never()).porGoogleId(anyString());
            verify(usuarios, never()).porCorreo(anyString());
        }
    }

    // ---------------- LOGIN GITHUB ----------------

    @Nested
    @DisplayName("loginGitHub")
    class LoginGitHub {

        private final GitHubUserInfo ghUser = new GitHubUserInfo(12345L, "octocat", "octo@example.com", "Octo Cat", "https://avatars/12345");

        @Test
        void usuarioGitHubExistenteLogeaSinConsultarPorCorreo() {
            Usuario u = buildUsuario("usr-001", null);
            when(gitHubVerifier.verificar(eq("code-1"), any())).thenReturn(ghUser);
            when(usuarios.porGitHubId(12345L)).thenReturn(Optional.of(u));

            AuthResult r = service.loginGitHub(new GitHubLoginCommand("code-1", "https://app/cb", null));
            assertNotNull(r);
            verify(usuarios, never()).porCorreo(anyString());
        }

        @Test
        void vinculaGitHubIdAUsuarioConCorreoExistente() {
            Usuario sinGitHub = buildUsuario("usr-001", "$2a$10$hashed");
            when(gitHubVerifier.verificar(eq("code-1"), any())).thenReturn(ghUser);
            when(usuarios.porGitHubId(12345L)).thenReturn(Optional.empty());
            when(usuarios.porCorreo("octo@example.com")).thenReturn(Optional.of(sinGitHub));
            when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(sinGitHub));

            AuthResult r = service.loginGitHub(new GitHubLoginCommand("code-1", "https://app/cb", null));
            assertNotNull(r);
            verify(usuarios).guardar(any(Usuario.class));
        }

        @Test
        void rechazaTakeoverCuandoGitHubIdYaExiste() {
            Usuario base = buildUsuario("usr-001", "$2a$10$hashed");
            Usuario conOtroGitHub = new Usuario(
                    base.id(), base.nombre(), base.correo(),
                    base.contrasenaHash(), base.rol(), base.telefono(),
                    base.avatar(), base.googleId(),
                    99999L,
                    base.fechaRegistro(), base.ultimoLogin(),
                    base.intentosFallidos(), base.bloqueadoHasta()
            );
            when(gitHubVerifier.verificar(eq("code-1"), any())).thenReturn(ghUser);
            when(usuarios.porGitHubId(12345L)).thenReturn(Optional.empty());
            when(usuarios.porCorreo("octo@example.com")).thenReturn(Optional.of(conOtroGitHub));

            assertThrows(CuentaGitHubVinculadaException.class,
                    () -> service.loginGitHub(new GitHubLoginCommand("code-1", "https://app/cb", null)));
        }

        @Test
        void creaUsuarioNuevoGitHub() {
            when(gitHubVerifier.verificar(eq("code-1"), any())).thenReturn(ghUser);
            when(usuarios.porGitHubId(12345L)).thenReturn(Optional.empty());
            when(usuarios.porCorreo("octo@example.com")).thenReturn(Optional.empty());
            when(sequenceGenerator.siguienteUsuarioId()).thenReturn("usr-079");
            when(usuarios.guardar(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(usuarios.porId("usr-079")).thenReturn(Optional.of(
                    new Usuario(new UsuarioId("usr-079"), "Octo Cat", "octo@example.com", null, RolUsuario.INQUILINO, null, "https://avatars/12345", null, 12345L, Instant.now(), null, 0, null)
            ));

            AuthResult r = service.loginGitHub(new GitHubLoginCommand("code-1", "https://app/cb", RolUsuario.INQUILINO));
            assertNotNull(r);
            verify(usuarios).guardar(any(Usuario.class));
            verify(sequenceGenerator).siguienteUsuarioId();
        }

        @Test
        void creaUsuarioGitHubConEmailNuloAhoraFallaAntesDeGuardar() {
            // Con el fix de validación: si el email es null, no se llega a guardar.
            // Este test ahora valida que el rechazo ocurre ANTES del lookup de GitHubId
            // (mismo comportamiento que rechazaEmailOcultoEnGitHub pero más específico).
            GitHubUserInfo sinEmail = new GitHubUserInfo(12345L, "octocat", null, "Octo Cat", "https://avatars/12345");
            when(gitHubVerifier.verificar(eq("code-1"), any())).thenReturn(sinEmail);

            assertThrows(IllegalArgumentException.class,
                    () -> service.loginGitHub(new GitHubLoginCommand("code-1", "https://app/cb", null)));
            verify(usuarios, never()).porGitHubId(any());
            verify(usuarios, never()).guardar(any(Usuario.class));
        }

        @Test
        void noConsultaRepositorioSiGitHubFalla() {
            when(gitHubVerifier.verificar(eq("bad"), any())).thenThrow(new IllegalArgumentException("bad code"));
            assertThrows(IllegalArgumentException.class,
                    () -> service.loginGitHub(new GitHubLoginCommand("bad", "https://app/cb", null)));
            verify(usuarios, never()).porGitHubId(any());
            verify(usuarios, never()).porCorreo(anyString());
        }

        @Test
        void rechazaEmailOcultoEnGitHub() {
            GitHubUserInfo sinEmail = new GitHubUserInfo(12345L, "octocat", null, "Octo Cat", "https://avatars/12345");
            when(gitHubVerifier.verificar(eq("code-1"), any())).thenReturn(sinEmail);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.loginGitHub(new GitHubLoginCommand("code-1", "https://app/cb", null)));
            assertTrue(ex.getMessage().contains("email público"));
            verify(usuarios, never()).porGitHubId(any());
            verify(usuarios, never()).porCorreo(anyString());
            verify(usuarios, never()).guardar(any(Usuario.class));
        }

        @Test
        void rechazaEmailVacioEnGitHub() {
            GitHubUserInfo emailVacio = new GitHubUserInfo(12345L, "octocat", "   ", "Octo Cat", "https://avatars/12345");
            when(gitHubVerifier.verificar(eq("code-1"), any())).thenReturn(emailVacio);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> service.loginGitHub(new GitHubLoginCommand("code-1", "https://app/cb", null)));
            assertTrue(ex.getMessage().contains("email público"));
        }
    }

    // ---------------- REFRESH ----------------

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        void refreshExitosoRevocaJtiViejoYDevuelveTokensNuevos() {
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(buildUsuario("usr-001", "$2a$10$hashed")));
            AuthResult r = service.refresh("usr-001", "old-jti");
            assertNotNull(r);
            verify(tokensRevocados).revocar(eq("old-jti"), any());
            verify(tokenProvider).generarRefreshToken(eq("usr-001"), anyString());
        }

        @Test
        void refreshDeCuentaBloqueadaLanza429() {
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(buildUsuarioBloqueado("usr-001")));
            assertThrows(CuentaBloqueadaException.class, () -> service.refresh("usr-001", "old-jti"));
        }

        @Test
        void refreshDeUsuarioInexistenteLanzaCredencialesInvalidas() {
            when(usuarios.porId("usr-XXX")).thenReturn(Optional.empty());
            assertThrows(CredencialesInvalidasException.class, () -> service.refresh("usr-XXX", null));
        }
    }

    // ---------------- PERFIL ----------------

    @Nested
    @DisplayName("perfil / obtener")
    class Perfil {

        @Test
        void perfilExistenteDevuelveView() {
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(buildUsuario("usr-001", "hash")));
            UsuarioView v = service.perfil("usr-001");
            assertEquals("usr-001", v.id());
        }

        @Test
        void perfilInexistenteLanza404() {
            when(usuarios.porId("usr-XXX")).thenReturn(Optional.empty());
            assertThrows(UsuarioNoEncontradoException.class, () -> service.perfil("usr-XXX"));
        }

        @Test
        void porIdExistenteDevuelveView() {
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(buildUsuario("usr-001", "hash")));
            UsuarioView v = service.porId("usr-001");
            assertEquals("usr-001", v.id());
        }

        @Test
        void porIdInexistenteLanza404() {
            when(usuarios.porId("usr-X")).thenReturn(Optional.empty());
            assertThrows(UsuarioNoEncontradoException.class, () -> service.porId("usr-X"));
        }
    }

    // ---------------- LISTAR ----------------

    @Nested
    @DisplayName("listar")
    class Listar {

        @Test
        void listarPaginadoCalculaPagesCorrectamente() {
            when(usuarios.paginado(1, 20)).thenReturn(List.of(
                    buildUsuario("usr-001", "h"), buildUsuario("usr-002", "h")));
            when(usuarios.contar()).thenReturn(45L);

            var result = service.listarPaginado(1, 20);
            assertEquals(2, result.data().size());
            assertEquals(45L, result.total());
            assertEquals(3, result.pages());
            assertEquals(1, result.page());
            assertEquals(20, result.size());
        }

        @Test
        void listarPaginadoLimitaSizeAMaximo100() {
            when(usuarios.paginado(1, 100)).thenReturn(List.of());
            when(usuarios.contar()).thenReturn(0L);
            service.listarPaginado(1, 999);
            verify(usuarios).paginado(1, 100);
        }

        @Test
        void listarPaginadoForzaPageMinimo1() {
            when(usuarios.paginado(1, 20)).thenReturn(List.of());
            when(usuarios.contar()).thenReturn(0L);
            service.listarPaginado(-5, 20);
            verify(usuarios).paginado(1, 20);
        }

        @Test
        void listarPorRolDevuelveListaDeViews() {
            when(usuarios.porRol(RolUsuario.DUENO)).thenReturn(List.of(buildUsuario("usr-001", "h")));
            List<UsuarioView> r = service.listarPorRol("dueno");
            assertEquals(1, r.size());
            assertEquals("usr-001", r.get(0).id());
        }

        @Test
        void listarPorRolInvalidoLanzaExcepcion() {
            assertThrows(IllegalArgumentException.class, () -> service.listarPorRol("admin"));
        }

        @Test
        void buscarPorPrefijoCorreoDevuelveLista() {
            when(usuarios.porPrefijoCorreo("juan")).thenReturn(List.of(buildUsuario("usr-001", "h")));
            assertEquals(1, service.buscarPorPrefijoCorreo("juan").size());
        }

        @Test
        void contarDevuelveLong() {
            when(usuarios.contar()).thenReturn(7L);
            assertEquals(7L, service.contar());
        }
    }

    // ---------------- ACTUALIZAR ----------------

    @Nested
    @DisplayName("actualizar")
    class Actualizar {

        @Test
        void actualizarExitosoDevuelveViewActualizada() {
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(buildUsuario("usr-001", "h")));
            when(usuarios.existeCorreoExcluyendoId("nuevo@example.com", "usr-001")).thenReturn(false);
            when(usuarios.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(buildUsuario("usr-001", "h")));

            UpdateUsuarioCommand cmd = new UpdateUsuarioCommand("Juan P", "nuevo@example.com", null, null);
            UsuarioView v = service.actualizar("usr-001", cmd, "usr-001");
            assertNotNull(v);
            verify(usuarios).guardar(any());
        }

        @Test
        void actualizarPorOtroUsuarioLanzaPermisoDenegado() {
            assertThrows(PermisoDenegadoException.class,
                    () -> service.actualizar("usr-001", new UpdateUsuarioCommand(null, null, null, null), "usr-002"));
            verify(usuarios, never()).guardar(any());
        }

        @Test
        void actualizarInexistenteLanza404() {
            when(usuarios.porId("usr-XXX")).thenReturn(Optional.empty());
            assertThrows(UsuarioNoEncontradoException.class,
                    () -> service.actualizar("usr-XXX", new UpdateUsuarioCommand(null, null, null, null), "usr-XXX"));
        }

        @Test
        void actualizarConCorreoExistenteLanza409() {
            when(usuarios.porId("usr-001")).thenReturn(Optional.of(buildUsuario("usr-001", "h")));
            when(usuarios.existeCorreoExcluyendoId("otro@example.com", "usr-001")).thenReturn(true);

            UpdateUsuarioCommand cmd = new UpdateUsuarioCommand(null, "otro@example.com", null, null);
            assertThrows(CorreoYaRegistradoException.class,
                    () -> service.actualizar("usr-001", cmd, "usr-001"));
        }
    }

    // ---------------- ELIMINAR ----------------

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        void eliminarPropiaCuentaExitoso() {
            when(usuarios.eliminar("usr-001")).thenReturn(true);
            service.eliminar("usr-001", "usr-001");
            verify(usuarios).eliminar("usr-001");
        }

        @Test
        void eliminarPorOtroUsuarioLanzaPermisoDenegado() {
            assertThrows(PermisoDenegadoException.class,
                    () -> service.eliminar("usr-001", "usr-002"));
            verify(usuarios, never()).eliminar(anyString());
        }

        @Test
        void eliminarUsuarioInexistenteLanza404() {
            when(usuarios.eliminar("usr-XXX")).thenReturn(false);
            assertThrows(UsuarioNoEncontradoException.class,
                    () -> service.eliminar("usr-XXX", "usr-XXX"));
        }
    }

    // ---------------- LOGOUT ----------------

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        void logoutConJtiRevocaToken() {
            service.logout("jti-123", Instant.now().plusSeconds(3600));
            verify(tokensRevocados).revocar(eq("jti-123"), any());
        }

        @Test
        void logoutSinJtiNoHaceNada() {
            service.logout(null, null);
            service.logout("", null);
            service.logout("   ", null);
            verify(tokensRevocados, never()).revocar(anyString(), any());
        }
    }
}
