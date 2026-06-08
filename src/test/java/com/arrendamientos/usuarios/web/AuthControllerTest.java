package com.arrendamientos.usuarios.web;

import com.arrendamientos.usuarios.domain.model.GoogleUserInfo;
import com.arrendamientos.usuarios.domain.model.RolUsuario;
import com.arrendamientos.usuarios.domain.model.Usuario;
import com.arrendamientos.usuarios.domain.model.UsuarioId;
import com.arrendamientos.usuarios.domain.model.UsuarioView;
import com.arrendamientos.usuarios.domain.port.in.ActualizarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.EliminarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.EnviarVerificacionEmailUseCase;
import com.arrendamientos.usuarios.domain.port.in.ListarUsuariosUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginGoogleUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginUseCase;
import com.arrendamientos.usuarios.domain.port.in.LogoutUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerPerfilUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.RefreshTokenUseCase;
import com.arrendamientos.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.VerificarEmailUseCase;
import com.arrendamientos.usuarios.domain.port.out.GoogleTokenVerifierPort;
import com.arrendamientos.usuarios.testsupport.TestJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private LoginUseCase loginUseCase;
    @MockBean private LoginGoogleUseCase loginGoogleUseCase;
    @MockBean private RegistrarUsuarioUseCase registrarUsuarioUseCase;
    @MockBean private ObtenerPerfilUseCase obtenerPerfilUseCase;
    @MockBean private LogoutUseCase logoutUseCase;
    @MockBean private VerificarEmailUseCase verificarEmailUseCase;
    @MockBean private EnviarVerificacionEmailUseCase enviarVerificacionEmailUseCase;
    @MockBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockBean private GoogleTokenVerifierPort googleVerifier;
    @MockBean private ListarUsuariosUseCase listarUsuariosUseCase;
    @MockBean private ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    @MockBean private ActualizarUsuarioUseCase actualizarUsuarioUseCase;
    @MockBean private EliminarUsuarioUseCase eliminarUsuarioUseCase;

    private static UsuarioView buildView(String id, RolUsuario rol) {
        return new UsuarioView(id, "Juan Pérez", "juan@example.com", rol, "+50688888888", null,
                Instant.parse("2024-01-01T00:00:00Z"), null);
    }

    // ============== LOGIN ==============

    @Nested
    class LoginEndpoint {

        @Test
        void loginExitoso() throws Exception {
            when(loginUseCase.login(any())).thenReturn(
                    new com.arrendamientos.usuarios.application.dto.AuthResult(
                            "access-token", "refresh-token", buildView("usr-001", RolUsuario.DUENO)));

            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"correo\":\"juan@example.com\",\"contrasena\":\"Password123!\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.usuario.id").value("usr-001"));
        }

        @Test
        void loginFallaPorValidacion() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"correo\":\"no-correo\",\"contrasena\":\"x\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Error"));
        }

        @Test
        void loginNormalizaEmailAMinusculas() throws Exception {
            when(loginUseCase.login(any())).thenReturn(
                    new com.arrendamientos.usuarios.application.dto.AuthResult(
                            "t", "r", buildView("usr-001", RolUsuario.DUENO)));

            mockMvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content("{\"correo\":\"JUAN@Example.COM\",\"contrasena\":\"Password123!\"}"))
                    .andExpect(status().isOk());

            org.mockito.ArgumentCaptor<com.arrendamientos.usuarios.application.dto.LoginCommand> captor =
                    org.mockito.ArgumentCaptor.forClass(com.arrendamientos.usuarios.application.dto.LoginCommand.class);
            verify(loginUseCase).login(captor.capture());
            org.junit.jupiter.api.Assertions.assertEquals("juan@example.com", captor.getValue().correo());
        }
    }

    // ============== REGISTRO ==============

    @Nested
    class RegistroEndpoint {

        @Test
        void registroExitoso201() throws Exception {
            when(registrarUsuarioUseCase.registrar(any())).thenReturn(
                    new com.arrendamientos.usuarios.application.dto.AuthResult(
                            "t", "r", buildView("usr-042", RolUsuario.DUENO)));

            mockMvc.perform(post("/api/auth/registro")
                            .contentType("application/json")
                            .content("{\"nombre\":\"Juan Pérez\",\"correo\":\"juan@example.com\",\"contrasena\":\"Password123!\",\"rol\":\"dueno\",\"telefono\":\"+50688888888\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.usuario.id").value("usr-042"));
        }

        @Test
        void registroSinNombreFallaValidacion() throws Exception {
            mockMvc.perform(post("/api/auth/registro")
                            .contentType("application/json")
                            .content("{\"correo\":\"juan@example.com\",\"contrasena\":\"Password123!\",\"rol\":\"dueno\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void registroContrasenaCorta() throws Exception {
            mockMvc.perform(post("/api/auth/registro")
                            .contentType("application/json")
                            .content("{\"nombre\":\"Juan\",\"correo\":\"juan@example.com\",\"contrasena\":\"123\",\"rol\":\"dueno\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============== GOOGLE ==============

    @Nested
    class GoogleEndpoint {

        @Test
        void googleLoginExitoso() throws Exception {
            when(googleVerifier.verificar(anyString(), any(), any())).thenReturn(
                    new GoogleUserInfo("g-sub", "g@e.com", "G", null));
            when(loginGoogleUseCase.loginGoogle(any())).thenReturn(
                    new com.arrendamientos.usuarios.application.dto.AuthResult(
                            "t", "r", buildView("usr-001", RolUsuario.DUENO)));

            mockMvc.perform(post("/api/auth/google")
                            .contentType("application/json")
                            .content("{\"googleToken\":\"tok\",\"rol\":\"inquilino\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("t"));
        }

        @Test
        void googleSinTokenFallaValidacion() throws Exception {
            mockMvc.perform(post("/api/auth/google")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ============== PROFILE ==============

    @Nested
    class ProfileEndpoint {

        @Test
        void profileSinToken401() throws Exception {
            mockMvc.perform(get("/api/auth/profile"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void profileConToken200() throws Exception {
            String token = TestJwt.accessToken("usr-001", "juan@example.com", RolUsuario.DUENO);
            when(obtenerPerfilUseCase.perfil("usr-001")).thenReturn(buildView("usr-001", RolUsuario.DUENO));

            mockMvc.perform(get("/api/auth/profile")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("usr-001"))
                    .andExpect(jsonPath("$.rol").value("dueno"));
        }

        @Test
        void profileConTokenInvalido401() throws Exception {
            mockMvc.perform(get("/api/auth/profile")
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ============== REFRESH ==============

    @Nested
    class RefreshEndpoint {

        @Test
        void refreshSinToken401() throws Exception {
            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void refreshConTokenDevuelveNuevosTokens() throws Exception {
            String token = TestJwt.accessToken("usr-001", "j@e.com", RolUsuario.DUENO);
            when(refreshTokenUseCase.refresh(anyString(), any())).thenReturn(
                    new com.arrendamientos.usuarios.application.dto.AuthResult(
                            "nuevo-access", "nuevo-refresh", buildView("usr-001", RolUsuario.DUENO)));

            mockMvc.perform(post("/api/auth/refresh")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("nuevo-access"));
        }
    }

    // ============== LOGOUT ==============

    @Nested
    class LogoutEndpoint {

        @Test
        void logoutSinToken401() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void logoutConToken200() throws Exception {
            String token = TestJwt.accessTokenWithJti("usr-001", "j@e.com", RolUsuario.DUENO, "jti-123");

            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logout exitoso. Sesión revocada."));

            verify(logoutUseCase).logout(any(), any());
        }
    }

    // ============== VERIFY EMAIL ==============

    @Nested
    class VerifyEmailEndpoint {

        @Test
        void verifyEmail200() throws Exception {
            when(verificarEmailUseCase.verificar(anyString())).thenReturn(
                    new VerificarEmailUseCase.Resultado("usr-001", "j@e.com"));

            mockMvc.perform(get("/api/auth/verify-email/token-x"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("usr-001"))
                    .andExpect(jsonPath("$.correo").value("j@e.com"));
        }
    }

    // ============== SEND VERIFICATION EMAIL ==============

    @Nested
    class SendVerificationEmailEndpoint {

        @Test
        void sendVerificacionSinToken401() throws Exception {
            mockMvc.perform(post("/api/auth/send-verification-email"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void sendVerificacionConToken200() throws Exception {
            String token = TestJwt.accessToken("usr-001", "j@e.com", RolUsuario.DUENO);

            mockMvc.perform(post("/api/auth/send-verification-email")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Email de verificación enviado"));

            verify(enviarVerificacionEmailUseCase).enviar(anyString(), anyString());
        }
    }
}
