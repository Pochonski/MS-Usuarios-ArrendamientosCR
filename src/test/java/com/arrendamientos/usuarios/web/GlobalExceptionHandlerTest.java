package com.arrendamientos.usuarios.web;

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
import com.arrendamientos.usuarios.domain.port.out.EmailSenderPort;
import com.arrendamientos.usuarios.domain.port.out.GoogleTokenVerifierPort;
import com.arrendamientos.usuarios.domain.port.out.PasswordEncoderPort;
import com.arrendamientos.usuarios.domain.port.out.SequenceGeneratorPort;
import com.arrendamientos.usuarios.domain.port.out.TokenProviderPort;
import com.arrendamientos.usuarios.domain.port.out.TokenRevocadoRepositoryPort;
import com.arrendamientos.usuarios.domain.port.out.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del GlobalExceptionHandler.
 * Cubre el caso de JSON malformado retornando 400 en lugar de 500 (W1.1).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // Mocks de TODAS las dependencias de AuthController y UsuarioController
    // (necesarios para que cargue el contexto sin intentar conectar a SQL Server)
    @MockBean private LoginUseCase loginUseCase;
    @MockBean private LoginGoogleUseCase loginGoogleUseCase;
    @MockBean private RegistrarUsuarioUseCase registrarUsuarioUseCase;
    @MockBean private ObtenerPerfilUseCase obtenerPerfilUseCase;
    @MockBean private LogoutUseCase logoutUseCase;
    @MockBean private VerificarEmailUseCase verificarEmailUseCase;
    @MockBean private EnviarVerificacionEmailUseCase enviarVerificacionEmailUseCase;
    @MockBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockBean private ListarUsuariosUseCase listarUsuariosUseCase;
    @MockBean private ObtenerUsuarioUseCase obtenerUsuarioUseCase;
    @MockBean private ActualizarUsuarioUseCase actualizarUsuarioUseCase;
    @MockBean private EliminarUsuarioUseCase eliminarUsuarioUseCase;

    // Ports y otros (no requeridos para el test, pero el contexto los puede pedir)
    @MockBean private TokenProviderPort tokenProvider;
    @MockBean private GoogleTokenVerifierPort googleTokenVerifierPort;
    @MockBean private EmailSenderPort emailSenderPort;
    @MockBean private UsuarioRepositoryPort usuarioRepositoryPort;
    @MockBean private PasswordEncoderPort passwordEncoderPort;
    @MockBean private SequenceGeneratorPort sequenceGeneratorPort;
    @MockBean private TokenRevocadoRepositoryPort tokenRevocadoRepositoryPort;

    @Test
    void jsonMalformado_llaveIncompleta_retornaBadRequest() throws Exception {
        // JSON con llave abierta pero nunca cerrada
        String malformed = "{\"correo\": \"test@test.com\"";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformed))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.message").value("JSON malformado o cuerpo inválido"));
    }

    @Test
    void jsonMalformado_comaSuelta_retornaBadRequest() throws Exception {
        // JSON con coma suelta
        String malformed = "{\"correo\": , \"contrasena\": \"x\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformed))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BadRequest"));
    }

    @Test
    void bodyVacio_retornaBadRequest() throws Exception {
        // Body vacío
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BadRequest"));
    }
}
