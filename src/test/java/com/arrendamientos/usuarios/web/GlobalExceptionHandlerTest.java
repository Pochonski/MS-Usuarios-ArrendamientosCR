package com.arrendamientos.usuarios.web;

import com.arrendamientos.usuarios.domain.port.in.ActualizarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.EliminarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.EnviarVerificacionEmailUseCase;
import com.arrendamientos.usuarios.domain.port.in.ListarUsuariosUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginGitHubUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginGoogleUseCase;
import com.arrendamientos.usuarios.domain.port.in.LoginUseCase;
import com.arrendamientos.usuarios.domain.port.in.LogoutUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerPerfilUseCase;
import com.arrendamientos.usuarios.domain.port.in.ObtenerUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.RefreshTokenUseCase;
import com.arrendamientos.usuarios.domain.port.in.RegistrarUsuarioUseCase;
import com.arrendamientos.usuarios.domain.port.in.VerificarEmailUseCase;
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

    @MockBean
    private LoginUseCase loginUseCase;

    @MockBean
    private LoginGoogleUseCase loginGoogleUseCase;

    @MockBean
    private LoginGitHubUseCase loginGitHubUseCase;

    @MockBean
    private RegistrarUsuarioUseCase registrarUsuarioUseCase;

    @MockBean
    private ObtenerPerfilUseCase obtenerPerfilUseCase;

    @MockBean
    private LogoutUseCase logoutUseCase;

    @MockBean
    private VerificarEmailUseCase verificarEmailUseCase;

    @MockBean
    private EnviarVerificacionEmailUseCase enviarVerificacionEmailUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockBean
    private ListarUsuariosUseCase listarUsuariosUseCase;

    @MockBean
    private ObtenerUsuarioUseCase obtenerUsuarioUseCase;

    @MockBean
    private ActualizarUsuarioUseCase actualizarUsuarioUseCase;

    @MockBean
    private EliminarUsuarioUseCase eliminarUsuarioUseCase;

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
